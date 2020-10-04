package io.github.eirikh1996.movecraftspace.objects

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.serialization.ConfigurationSerializable
import kotlin.math.*

data class ImmutableVector constructor(val x : Int, val y : Int, val z : Int) : ConfigurationSerializable{
    fun add(x: Int, y: Int, z: Int) : ImmutableVector {
        return ImmutableVector(this.x + x, this.y + y, this.z + z)
    }

    fun subtract(x: Int, y: Int, z: Int) : ImmutableVector {
        return ImmutableVector(this.x - x, this.y - y, this.z - z)
    }

    fun add(vec : ImmutableVector) : ImmutableVector {
        return add(vec.x, vec.y, vec.z)
    }

    fun subtract(vec : ImmutableVector) : ImmutableVector {
        return subtract(vec.x, vec.y, vec.z)
    }
    fun distance (other : ImmutableVector): Double {
        return sqrt(distanceSquared(other))
    }

    fun distanceSquared (other : ImmutableVector): Double {
        val dx = (other.x - x).toDouble()
        val dy = (other.y - y).toDouble()
        val dz = (other.z - z).toDouble()
        return ( dx * dx + dy * dy + dz * dz )
    }

    val length = sqrt((x * x + y * y + z * z).toDouble())

    fun rotate(angle : Double, origin : ImmutableVector) : ImmutableVector {
        val toRotate = subtract(origin)
        val cos = cos(angle)
        val sin = sin(angle)
        val x = round(toRotate.x * cos + toRotate.z * -sin).toInt()
        val z = round(toRotate.x * sin + toRotate.z * cos).toInt()
        return ImmutableVector(x, 0, z).add(origin)
    }

    fun normalize(): ImmutableVector {
        return ImmutableVector((x / length).toInt(), (y /length).toInt(), (z / length).toInt())
    }

    fun toLocation(w : World): Location {
        return Location(w, x.toDouble(), y.toDouble(), z.toDouble())
    }

    override fun serialize(): MutableMap<String, Any> {
        val map = HashMap<String, Any>()
        map.put("x", x)
        map.put("y", y)
        map.put("z", z)
        return map
    }

    override fun toString(): String {
        return "( " + x + ", " + y + ", " + z + ")"
    }

    companion object {
        fun deserialize(map : Map<String, Any>) : ImmutableVector {
            return ImmutableVector(map["x"] as Int, map["y"] as Int, map["z"] as Int)
        }

        fun fromLocation(loc : Location): ImmutableVector {
            return ImmutableVector(loc.blockX, loc.blockY, loc.blockZ)
        }
        val ZERO = ImmutableVector(0,0,0)
    }
}

