package io.github.eirikh1996.movecraftspace.objects

import net.countercraft.movecraft.MovecraftChunk
import org.bukkit.Bukkit
import org.bukkit.Bukkit.broadcastMessage
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.io.PrintWriter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.abs

object PlanetCollection : Iterable<Planet> {
    private val planets = HashSet<Planet>()
    lateinit var pl : Plugin

    fun getPlanetsWithOrbitPoint(orbitPoint : ImmutableVector) : Set<Planet> {
        val returnSet = HashSet<Planet>()
        for (p in PlanetCollection) {
            if (!p.orbitCenter.equals(orbitPoint))
                continue
            returnSet.add(p)
        }
        return returnSet
    }

    fun nearestPlanet(space : World, loc : ImmutableVector, maxDistance : Int = -1, excludeMoons : Boolean = false) : Planet? {
        var foundPlanet : Planet? = null
        var lastDistance = Int.MAX_VALUE
        for (p in this) {
            if (!p.space.equals(space))
                continue
            if (p.center.distance(loc).toInt() > lastDistance || maxDistance > -1 && p.center.distance(loc).toInt() - p.moonOrbitRadius() > maxDistance)
                continue
            if (p.isMoon() && excludeMoons)
                continue
            foundPlanet = p
            lastDistance = p.center.distance(loc).toInt()
        }
        return foundPlanet
    }

    fun getPlanetAt(loc : Location, extraRadius : Int = 0) : Planet? {
        for (planet in planets) {
            if (planet.isPlanet(loc.world!!) || planet.contains(loc, extraRadius)) {
                return planet;
            }
        }
        return null;
    }

    fun getCorrespondingPlanet(world: World) : Planet? {
        for (planet in planets) {
            if (!planet.destination.equals(world))
                continue
            return planet
        }
        return null
    }

    fun getPlanetByName(name : String) : Planet? {
        for (planet in planets) {
            if (planet.destination.name.equals(name, true)) {
                return planet;
            }
        }
        return null;
    }

    fun contains (planet: Planet) : Boolean {
        return planets.contains(planet)
    }

    fun add (planet: Planet) {
        planets.add(planet)
        saveFile()
    }

    fun remove (planet: Planet) {
        planets.remove(planet)
        saveFile()
    }

    fun loadPlanets() {
        val file = File(pl.dataFolder, "planets.yml")
        if (!file.exists())
            return
        val yaml = Yaml()
        val data = yaml.load<Map<String, Any>>(FileInputStream(file))
        val planetData = data.get("planets") as Map<String, Any>
        val moonMap = HashMap<Planet, List<String>>()
        for (entry in planetData.entries) {
            val entryData = entry.value as Map<String, Any>
            val destination = Bukkit.getWorld(entry.key)

            if (destination == null)
                continue
            val centerData = entryData.get("center") as Map<String, Any>
            val orbitCenterData = entryData.get("orbitCenter") as Map<String, Any>
            val moons = entryData.getOrDefault("moons", ArrayList<String>()) as List<String>
            val planet = Planet(
                ImmutableVector.deserialize(centerData),
                ImmutableVector.deserialize(orbitCenterData),
                entryData.get("radius") as Int,
                Bukkit.getWorld(entryData["space"] as String)!!,
                destination,
                entryData.get("orbitTime") as Int,
                entryData.get("exitHeight") as Int)
            planets.add(planet)
            moonMap.put(planet, moons)
        }
        for (planet in moonMap.entries) {

            for (moonName in planet.value) {
                val moon = getPlanetByName(moonName)
                if (moon == null) continue
                planet.key.moons.add(moon)
            }
            if (planet.key.moons.isEmpty())
                continue
            pl.logger.info("Loaded " + planet.key.moons.size + " moons for planet " + planet.key.destination.name)
        }
        pl.logger.info("Loaded " + planets.size + " planets")
    }

    fun withinPlanetOrbit(loc : Location): Boolean {
        for (planet in this) {
            val orbitCenterDistance = planet.orbitCenter.distance(planet.center)
            val minDistance = orbitCenterDistance - planet.radius
            val maxDistance = orbitCenterDistance + planet.radius
            val distanceToLocation = ImmutableVector.fromLocation(loc).distance(planet.orbitCenter)
            if (distanceToLocation < minDistance || distanceToLocation > maxDistance)
                continue
            return true
        }
        return false
    }

    fun saveFile() {
        if (planets.isEmpty())
            return
        val file = File(pl.dataFolder, "planets.yml")
        if (!file.exists())
            file.createNewFile()
        val printWriter = PrintWriter(file)
        printWriter.println("#Do not edit this file, or your data may be corrupted")
        printWriter.println("planets:")
        for (planet in planets) {
            val center = planet.center
            val orbitCenter = planet.orbitCenter
            printWriter.println("    " + planet.destination.name + ":")
            printWriter.println("        space: " + planet.space.name)
            printWriter.println("        center:")
            printWriter.println("            x: " + center.x)
            printWriter.println("            \'y\': " + center.y)
            printWriter.println("            z: " + center.z)
            printWriter.println("        orbitCenter:")
            printWriter.println("            x: " + orbitCenter.x)
            printWriter.println("            \'y\': " + orbitCenter.y)
            printWriter.println("            z: " + orbitCenter.z)
            printWriter.println("        orbitTime: " + planet.orbitTime)
            printWriter.println("        exitHeight: " + planet.exitHeight)
            printWriter.println("        radius: " + planet.radius)
            if (planet.moons.isEmpty())
                continue
            printWriter.println("        moons:")
            for (moon in planet.moons) {
                printWriter.println("        - " + moon.destination.name)
            }
        }
        printWriter.close()
    }
    fun intersectingOtherPlanetaryOrbit(planet: Planet, moonCenter : Planet? = null) : Planet? {
        for (pl in this) {
            if (pl == planet || moonCenter == pl)
                continue
            val distance = pl.orbitCenter.distance(pl.center)
            val minDistance = distance - pl.radius
            val maxDistance = distance + pl.radius
            val planetDistance = pl.orbitCenter.distance(planet.orbitCenter)
            if (planetDistance >= minDistance && planetDistance <= maxDistance)
                return pl


        }
        return null
    }
    fun intersectingOtherPlanetaryOrbit(chunk : MovecraftChunk) : Planet? {
        var foundPlanet : Planet? = null
        val maxX = chunk.x * 16
        val maxZ = chunk.z * 16
        val minX = maxX - 15
        val minZ = maxZ - 15
        for (x in minX..maxX)
            for (z in minZ..maxZ) {
                val p = intersectingOtherPlanetaryOrbit(Location(chunk.world, x.toDouble(), 127.5, z.toDouble()))
                if (p != null) {
                    foundPlanet = p
                    break
                }
            }
        return foundPlanet
    }

    fun intersectingOtherPlanetaryOrbit(loc : Location) : Planet? {
        for (pl in this) {
            if (pl.space != loc.world)
                continue
            val distance = pl.orbitCenter.distance(pl.center)
            val minDistance = distance - pl.radius
            val maxDistance = distance + pl.radius
            val planetDistance = pl.orbitCenter.distance(ImmutableVector.fromLocation(loc))
            if (planetDistance >= minDistance && planetDistance <= maxDistance)
                return pl

        }
        return null
    }

    override fun iterator() : Iterator<Planet> {
        return Collections.unmodifiableSet(planets).iterator()
    }

    fun worldIsPlanet(world: World): Boolean {
        for (p in this) {
            if (p.destination.equals(world))
                return true
        }
        return false
    }
}