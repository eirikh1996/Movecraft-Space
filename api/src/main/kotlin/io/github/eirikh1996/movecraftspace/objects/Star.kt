package io.github.eirikh1996.movecraftspace.objects

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World

data class Star constructor(val name : String, val space : World,val loc : ImmutableVector) {

    fun radius() : Int {
        var y = loc.y
        var test = Material.AIR
        while (test == Material.AIR) {
            y++
            test = space.getBlockAt(loc.x, y, loc.z).type
        }
        var radius = y - loc.y
        for (p in PlanetCollection.getPlanetsWithOrbitPoint(loc)) {
            if (p.orbitRadius <= radius)
                continue

            radius = p.orbitRadius
            radius += p.radius
            if (p.moons.isEmpty())
                continue
            var moonRadius = 0
            for (moon in p.moons) {
                val distance = moon.center.distance(p.center).toInt()
                if (distance > moonRadius)
                    continue
                moonRadius = distance
                moonRadius += moon.radius
            }
            radius += moonRadius
        }
        return radius
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Star)
            return false
        return other.name == name && other.loc == loc
    }

    override fun hashCode(): Int {
        return name.hashCode() + loc.hashCode()
    }
}