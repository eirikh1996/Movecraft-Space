package io.github.eirikh1996.movecraftspace.expansion.selection

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector.Axis
import io.github.eirikh1996.movecraftspace.objects.MSBlock
import net.countercraft.movecraft.MovecraftLocation
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.WallSign
import org.bukkit.configuration.serialization.ConfigurationSerializable
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

abstract class Structure(val name : String) : ConfigurationSerializable {
    var blocks : MutableMap<MovecraftLocation, MSBlock> = HashMap()
    val zeroVector = MovecraftLocation(0, 0, 0)

    val minX : Int get() {
        var lastX = Int.MAX_VALUE
        for (vec in blocks.keys) {
            if (lastX <= vec.x)
                continue
            lastX = vec.x
        }
        return lastX
    }

    val maxX : Int get() {
        var lastX = Int.MIN_VALUE
        for (vec in blocks.keys) {
            if (lastX >= vec.x)
                continue
            lastX = vec.x
        }
        return lastX
    }

    val minY : Int get() {
        var lastY = Int.MAX_VALUE
        for (vec in blocks.keys) {
            if (lastY <= vec.y)
                continue
            lastY = vec.y
        }
        return lastY
    }

    val maxY : Int get() {
        var lastY = Int.MIN_VALUE
        for (vec in blocks.keys) {
            if (lastY >= vec.y)
                continue
            lastY = vec.y
        }
        return lastY
    }

    val minZ : Int get() {
        var lastZ = Int.MAX_VALUE
        for (vec in blocks.keys) {
            if (lastZ <= vec.z)
                continue
            lastZ = vec.z
        }
        return lastZ
    }

    val maxZ : Int get() {
        var lastZ = Int.MIN_VALUE
        for (vec in blocks.keys) {
            if (lastZ >= vec.z)
                continue
            lastZ = vec.z
        }
        return lastZ
    }

    val xLength : Int get() = abs(maxX - minX)
    val yLength : Int get() = abs(maxY - minY)
    val zLength : Int get() = abs(maxZ - minZ)

    fun copy(selection: Selection, origin : MovecraftLocation = selection.center) {
        for (loc in selection) {
            val block = loc.toBukkit(selection.world).block
            if (block.type.isAir)
                continue
            blocks[loc.subtract(origin)] = MSBlock.fromBlock(block)

        }
    }

    fun paste(target : Location) {
        for (vec in blocks.keys) {
            val block = blocks[vec]!!
            val b = target.world!!.getBlockAt(target.clone().add(vec.toBukkit(target.world!!)))
            b.blockData = block.data
        }
    }

    abstract fun save()

    override fun serialize(): MutableMap<String, Any> {
        val data = HashMap<String, Any>()
        data["name"] = name
        val mapList = ArrayList<Map<String, Any>>()
        blocks.forEach { (t, u) ->
            val blockData = HashMap<String, Any>()
            val locationData = HashMap<String, Any>()
            locationData["x"] = t.x
            locationData["y"] = t.y
            locationData["z"] = t.z
            blockData.putAll(locationData)
            blockData.putAll(u.serialize())
            mapList.add(blockData)
        }
        data["blocks"] = mapList
        return data
    }

    /**
     * Rotates a MovecraftLocation around a given origin
     * @param angle The rotational angle
     * @param origin The origin of the rotation
     * @param offset The location offset from the origin
     */
    protected fun rotate(angle : Double, origin : MovecraftLocation, offset : MovecraftLocation) : MovecraftLocation {
        val toRotate = offset.subtract(origin)
        val cos = cos(angle)
        val sin = sin(angle)
        val x : Int = round(toRotate.x * cos + toRotate.z * -sin).toInt()
        val y : Int = 0
        val z : Int = round(toRotate.x * sin + toRotate.z * cos).toInt()

        return MovecraftLocation(x, y, z).add(origin)
    }




}