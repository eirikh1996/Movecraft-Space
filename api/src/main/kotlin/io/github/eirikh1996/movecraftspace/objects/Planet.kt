package io.github.eirikh1996.movecraftspace.objects

import io.github.eirikh1996.movecraftspace.Settings
import net.countercraft.movecraft.Movecraft
import net.countercraft.movecraft.MovecraftLocation
import net.countercraft.movecraft.WorldHandler
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.util.Vector
import java.lang.Exception
import java.lang.reflect.Method
import java.util.*
import java.util.logging.Logger
import kotlin.collections.HashSet
import kotlin.math.min

data class Planet(var center: ImmutableVector, var orbitCenter : ImmutableVector, val radius : Int, val space : World, val destination : World, val orbitTime : Int, val exitHeight : Int) {
    var setBlockFast : Method? = null
    init {
        try {
            setBlockFast = WorldHandler::class.java.getDeclaredMethod("setBlockFast", Location::class.java, Material::class.java, Any::class.java)
        } catch (e : Exception) {

        }
    }

    val moons = HashSet<Planet>()
    val id = UUID.randomUUID()
    fun contains(location: Location) : Boolean {
        if (!space.equals(location.world)) {
            return false
        }
        return center.distance(ImmutableVector.fromLocation(location)) <= radius
    }

    fun isPlanet(world: World) : Boolean {
        return destination.equals(world)
    }

    fun moonOrbitRadius(): Int {
        if (moons.isEmpty())
            return radius
        var orbitRadius = radius
        for (m in moons) {
            if (m.center.distance(center).toInt() < orbitRadius)
                continue
            orbitRadius = m.center.distance(center).toInt()
        }
        return orbitRadius
    }

    fun isMoon() : Boolean {
        for (pl in PlanetCollection) {
            if (pl.moons.contains(this))
                return true
        }
        return false
    }
    override fun hashCode(): Int {
        return destination.hashCode() + space.hashCode() + id.hashCode()
    }

    var minX = center.x - radius
    var maxX = center.x + radius
    var minY = center.y - radius
    var maxY = center.y + radius
    var minZ = center.z - radius
    var maxz = center.z + radius

    override fun equals(other: Any?): Boolean {
        if (other !is Planet) {
            return false
        }
        return destination.equals(other.destination) && space.equals(other.space) && id.equals(other.id)
    }

    fun move(displacement : ImmutableVector) {
        move(displacement, false)
    }

    fun move(displacement : ImmutableVector, moon : Boolean) {
        var type = center.toLocation(space).block.type
        var y = center.y
        while (type == Material.AIR && y <= 255) {
            y++
            type = space.getBlockAt(center.x, min(y, 255), center.z).type
        }
        val start = ImmutableVector(center.x, y, center.z)
        val queue = LinkedList<ImmutableVector>()
        val visited = HashSet<ImmutableVector>()
        queue.add(start)
        while (!queue.isEmpty()) {
            val node = queue.poll()
            if (visited.contains(node))
                continue
            visited.add(node)
            for (shift in SHIFTS) {
                val test = node.add(shift)
                if (test.toLocation(space).block.type.name.endsWith("AIR"))
                    continue
                queue.add(test)
            }
        }
        Bukkit.broadcastMessage(visited.size.toString())
        Bukkit.broadcastMessage(destination.name)
        Bukkit.broadcastMessage("Moon: " + moon)
        Bukkit.broadcastMessage(displacement.toString())
        val newLocMap = HashMap<ImmutableVector, Pair<Material, Any>>()
        for (loc in visited) {
            val b = loc.toLocation(space).block
            newLocMap.put(loc.add(displacement), Pair(b.type, if (Settings.IsLegacy) b.data else b.blockData))
        }
        for (loc in newLocMap.keys) {
            val pair = newLocMap[loc]!!
            val b = loc.toLocation(space).block
            b.setType(pair.first)
            setBlock(b.location, pair.first, pair.second)
        }
        for (loc in visited.filter { vec -> !newLocMap.containsKey(vec) }) {
            setBlock(loc.toLocation(space), Material.AIR, if (Settings.IsLegacy) 0.toByte() else Bukkit.createBlockData(
                Material.AIR))
        }
        center = center.add(displacement)
        if (moon)
            orbitCenter = orbitCenter.add(displacement)
        minX = center.x - radius
        maxX = center.x + radius
        minY = center.y - radius
        maxY = center.y + radius
        minZ = center.z - radius
        maxz = center.z + radius
        PlanetCollection.saveFile()
    }

    var orbitRadius : Int get() { return center.distance(orbitCenter).toInt() } set(value) { throw UnsupportedOperationException()}

    private fun setBlock(loc : Location, type : Material, data : Any) {
        val wh = Movecraft.getInstance().worldHandler
        if (Settings.IsLegacy) {
            wh.setBlockFast(loc, type, data as Byte)
        } else {
            setBlockFast!!.invoke(wh, loc, type, data)
        }

    }

    private val SHIFTS = arrayOf(
        ImmutableVector(1,1,0),
        ImmutableVector(1,0,0),
        ImmutableVector(1,-1,0),
        ImmutableVector(-1,1,0),
        ImmutableVector(-1,0,0),
        ImmutableVector(-1,-1,0),
        ImmutableVector(0,1,1),
        ImmutableVector(0,0,1),
        ImmutableVector(0,-1,1),
        ImmutableVector(0,1,-1),
        ImmutableVector(0,0,-1),
        ImmutableVector(0,-1,-1),
        ImmutableVector(0,1,0),
        ImmutableVector(0,-1,0)
    )
}