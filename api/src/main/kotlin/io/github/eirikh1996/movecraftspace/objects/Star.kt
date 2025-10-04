package io.github.eirikh1996.movecraftspace.objects

import io.github.eirikh1996.movecraftspace.event.planet.PlanetMoveEvent
import net.countercraft.movecraft.Movecraft
import net.countercraft.movecraft.MovecraftLocation
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.data.BlockData
import java.util.*
import kotlin.collections.HashSet
import kotlin.math.min

class Star(val name : String, space : World, var loc : MovecraftLocation, val radius : Int) {

    var space : World = space
        get() = field
        private set(value) { field = value }
    fun radius() : Int {
        var radius = this.radius
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