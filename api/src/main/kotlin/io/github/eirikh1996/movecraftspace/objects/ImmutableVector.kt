package io.github.eirikh1996.movecraftspace.objects

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.util.Vector
import kotlin.math.*

@Deprecated(
    message = "Use MovecraftLocation instead"
)
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


    val bukkitVector : Vector get() = Vector(x, y, z)

    enum class Axis {
        X, Y, Z
    }

    fun rotate(angle : Double, origin : ImmutableVector, axis: Axis = Axis.Y) : ImmutableVector {
        val toRotate = subtract(origin)
        val cos = cos(angle)
        val sin = sin(angle)
        val x : Int
        val y : Int
        val z : Int
        when (axis) {
            Axis.X -> {
                x = 0
                y = round(toRotate.y * cos + toRotate.y * sin).toInt()
                z = round(toRotate.x * -sin + toRotate.z * cos).toInt()
            }
            Axis.Y -> {
                x = round(toRotate.x * cos + toRotate.z * -sin).toInt()
                y = 0
                z = round(toRotate.x * sin + toRotate.z * cos).toInt()
            }
            Axis.Z -> {
                x = round(toRotate.x * cos + toRotate.y * -sin).toInt()
                y = round(toRotate.x * sin + toRotate.y * cos).toInt()
                z = 0
            }
        }

        return ImmutableVector(x, y, z).add(origin)
    }

    fun normalize(): ImmutableVector {
        return ImmutableVector((x / length).toInt(), (y /length).toInt(), (z / length).toInt())
    }

    fun toLocation(w : World): Location {
        return Location(w, x.toDouble(), y.toDouble(), z.toDouble())
    }

    override fun serialize(): MutableMap<String, Any> {
        val map = HashMap<String, Any>()
        map["x"] = x
        map["y"] = y
        map["z"] = z
        return map
    }

    override fun toString(): String {
        return "( $x, $y, $z)"
    }

    fun dot(other: Immutable2dVector) : Int{
        return x * other.x + z * other.z
    }

    fun multiply(value : Int): Immutable2dVector {
        return Immutable2dVector(x * value, z * value)
    }

    fun divide(value : Int): Immutable2dVector {
        return Immutable2dVector(x / value, z / value)
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

