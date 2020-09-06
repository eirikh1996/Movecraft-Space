package io.github.eirikh1996.movecraftspace.utils

import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import net.countercraft.movecraft.WorldHandler
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.scheduler.BukkitRunnable
import java.lang.Exception
import java.lang.reflect.Method
import kotlin.math.abs
import kotlin.random.Random

object MSUtils {

    fun randomCoords(minX : Int, maxX : Int, minY : Int, maxY : Int, minZ : Int, maxZ : Int) : IntArray {

        return intArrayOf(Random.nextInt(minX, maxX), Random.nextInt(minY, maxY), Random.nextInt(minZ, maxZ))
    }


    fun createSphere(radius : Int, center : ImmutableVector) : Set<ImmutableVector> {
        val minX = center.x - radius
        val maxX = center.x + radius
        val minZ = center.z - radius
        val maxZ = center.z + radius
        val radiusSquared = radius * radius
        val sphere = HashSet<ImmutableVector>()
        for (y in 0..255) {
            for (x in minX..maxX) {
                for (z in minZ..maxZ) {
                    val vec = ImmutableVector(x, y, z)
                    if (abs(vec.distanceSquared(center) - radiusSquared) > radius)
                        continue
                    sphere.add(vec)
                }
            }
        }
        return sphere

    }

    val COMMAND_PREFIX = "§5[§9Movecraft-Space§5]§r "
    val ERROR = "§4Error: "
}