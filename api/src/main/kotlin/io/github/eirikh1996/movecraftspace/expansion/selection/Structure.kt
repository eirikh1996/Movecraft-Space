package io.github.eirikh1996.movecraftspace.expansion.selection

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.MSBlock
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.WallSign
import org.bukkit.configuration.serialization.ConfigurationSerializable
import kotlin.math.abs

abstract class Structure(val name : String) : ConfigurationSerializable {
    var blocks : MutableMap<ImmutableVector, MSBlock> = HashMap()

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

    fun copy(selection: Selection, origin : ImmutableVector = selection.center) {
        for (loc in selection) {
            val block = loc.toLocation(selection.world).block
            if (block.type.isAir)
                continue
            blocks[loc.subtract(origin)] = MSBlock.fromBlock(block)

        }
    }

    fun paste(target : Location) {
        for (vec in blocks.keys) {
            val block = blocks[vec]!!
            val b = target.world!!.getBlockAt(target.clone().add(vec.toLocation(target.world!!)))
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
            blockData.putAll(t.serialize())
            blockData.putAll(u.serialize())
            mapList.add(blockData)
        }
        data["blocks"] = mapList
        return data
    }




}