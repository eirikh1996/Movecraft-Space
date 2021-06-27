package io.github.eirikh1996.movecraftspace.expansion.selection

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.MSBlock
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.configuration.serialization.ConfigurationSerializable

abstract class Structure : ConfigurationSerializable {
    var blocks : MutableMap<ImmutableVector, MSBlock> = HashMap()

    fun copy(selection: Selection, origin : ImmutableVector = selection.center) {
        for (loc in selection) {
            val block = loc.toLocation(selection.world).block
            if (block.type.name.endsWith("AIR"))
                continue
            blocks.put(loc.subtract(origin), MSBlock.fromBlock(block))

        }
    }

    fun paste(target : Location) {
        for (vec in blocks.keys) {
            val block = blocks[vec]!!
            val b = target.world!!.getBlockAt(target.clone().add(vec.toLocation(target.world!!)))
            if (Settings.IsLegacy) {
                b.type = block.type
                Block::class.java.getDeclaredMethod("setData", Byte::class.java).invoke(b, block.data)
            } else {
                b.blockData = block.data as BlockData
            }
        }
    }

    override fun serialize(): MutableMap<String, Any> {
        TODO("Not yet implemented")
    }


}