package io.github.eirikh1996.movecraftspace.objects

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.WorldBorder
import org.bukkit.util.Vector
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class MSWorldBorder(val center : Immutable2dVector, val xRadius : Double, val zRadius : Double, val ellipsoid : Boolean = false) {
    private val radiusQuotient = xRadius / zRadius

    private val xRadiusSquared = xRadius * xRadius

    private val zRadiusSquared = zRadius * zRadius

    constructor(center: Immutable2dVector, radius: Double, ellipsoid: Boolean = false) : this(center, radius, radius, ellipsoid)

    val minPoint : Immutable2dVector get() = Immutable2dVector((center.x - xRadius).toInt(), (center.z - zRadius).toInt())

    val maxPoint : Immutable2dVector get() = Immutable2dVector((center.x + xRadius).toInt(), (center.z + zRadius).toInt())

    fun nearestLocWithin(loc : Location, buffer : Int = 0) : Location {
        val vec = Immutable2dVector.fromLocation(loc)
        val dx = loc.x - center.x
        val dz = loc.z - center.z
        if (withinBorder(loc))
            return loc
        var newX = loc.x
        var newZ = loc.z
        if (ellipsoid) {
            val dxSquared = (dx * dx)
            val dzSquared = (dz * dz)
            val dU = sqrt(dxSquared + dzSquared)
            val dT = sqrt(dxSquared / xRadiusSquared + dzSquared / zRadiusSquared)
            val f = (1 / dT - buffer / dU)
            newX = (center.x + dx * f)
            newZ = (center.z + dz * f)

        } else {
            val bufferedXRadius = xRadius - buffer
            val bufferedZRadius = zRadius - buffer
            if (dx <= bufferedXRadius && dz <= bufferedZRadius)
                return loc
            if (dx > bufferedXRadius) {
                val corr = dx - bufferedXRadius
                newX = if (vec.x < center.x)
                    (vec.x + corr)
                else
                    (vec.x - corr)
            }
            if (dz > bufferedZRadius) {
                val corr = dz - bufferedZRadius
                newZ = if (vec.z < center.z)
                    (vec.z + corr)
                else
                    (vec.z - corr)

            }
        }
        return Location(loc.world, newX, loc.y, newZ)

    }

    fun withinBorder(loc : Location, buffer: Int = 0) : Boolean {
        val dx = abs(center.x - loc.blockX).toDouble()
        val dz = abs(center.z - loc.blockZ).toDouble()
        if (ellipsoid) {
            return sqrt((dx * dx + dz * dz)) * radiusQuotient <= xRadius - buffer
        }
        return dx <= xRadius - buffer && dz <= zRadius - buffer
    }
    companion object {
        fun fromBukkit(worldBorder : WorldBorder) = MSWorldBorder(Immutable2dVector.fromLocation(worldBorder.center), min(worldBorder.size / 2, 29999983.7), min(worldBorder.size / 2, 29999983.7))
    }
}