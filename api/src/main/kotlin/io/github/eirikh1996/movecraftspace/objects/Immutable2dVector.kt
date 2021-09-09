package io.github.eirikh1996.movecraftspace.objects

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.util.Vector
import kotlin.math.*

data class Immutable2dVector(val x : Int, val z : Int) : ConfigurationSerializable {
    fun add(x: Int, z: Int) : Immutable2dVector {
        return Immutable2dVector(this.x + x, this.z + z)
    }

    fun subtract(x: Int, z: Int) : Immutable2dVector {
        return Immutable2dVector(this.x - x, this.z - z)
    }

    fun add(vec : Immutable2dVector) : Immutable2dVector {
        return add(vec.x, vec.z)
    }

    fun subtract(vec : Immutable2dVector) : Immutable2dVector {
        return subtract(vec.x, vec.z)
    }
    fun distance (other : Immutable2dVector): Double {
        return sqrt(distanceSquared(other))
    }

    fun distanceSquared (other : Immutable2dVector): Double {
        val dx = (other.x - x).toDouble()
        val dz = (other.z - z).toDouble()
        return ( dx * dx + dz * dz )
    }

    val length = sqrt(abs(x * x + z * z).toDouble())

    fun rotate(angle : Double, origin : Immutable2dVector) : Immutable2dVector {
        val toRotate = subtract(origin)
        val cos = cos(angle)
        val sin = sin(angle)
        val x : Int = round(toRotate.x * cos + toRotate.z * -sin).toInt()
        val z : Int = round(toRotate.x * sin + toRotate.z * cos).toInt()
        return Immutable2dVector(x, z).add(origin)
    }

    fun normalize(): Immutable2dVector {
        return Immutable2dVector((x / length).toInt(), (z / length).toInt())
    }

    fun toLocation(w : World): Location {
        return Location(w, x.toDouble(), 0.0, z.toDouble())
    }

    override fun serialize(): MutableMap<String, Any> {
        val map = HashMap<String, Any>()
        map["x"] = x
        map["z"] = z
        return map
    }

    override fun toString(): String {
        return "( $x, $z)"
    }

    fun dot(other: ImmutableVector) : Int{
        return x * other.x + z * other.z
    }

    fun multiply(value : Int): Immutable2dVector {
        return Immutable2dVector(x * value,  z * value)
    }

    fun divide(value : Int): Immutable2dVector {
        return Immutable2dVector(x / value, z / value)
    }

    val bukkitVector : Vector get() = Vector(x, 0, z)

    companion object {
        @JvmStatic
        fun deserialize(map : Map<String, Any>) : Immutable2dVector {
            return Immutable2dVector(map["x"] as Int, map["z"] as Int)
        }

        fun fromLocation(loc : Location): Immutable2dVector {
            return Immutable2dVector(loc.blockX, loc.blockZ)
        }

        fun fromBukkitVector(vec : Vector) : Immutable2dVector {
            return Immutable2dVector(vec.blockX, vec.blockZ)
        }
        val ZERO = Immutable2dVector(0,0)
    }
}
