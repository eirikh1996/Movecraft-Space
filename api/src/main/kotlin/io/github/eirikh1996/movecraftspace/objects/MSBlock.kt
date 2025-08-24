package io.github.eirikh1996.movecraftspace.objects

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.utils.BlockUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Bisected
import org.bukkit.block.data.BlockData
import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.material.Directional

data class MSBlock(val type : Material, val data : BlockData = Bukkit.createBlockData(type), val facing : BlockFace = BlockFace.SELF) : ConfigurationSerializable {

    override fun serialize(): MutableMap<String, Any> {
        val map = HashMap<String, Any>()
        map["type"] = type.name
        map["data"] = data.asString
        map["facing"] = facing.name
        return map;
    }

    fun rotate(facing : BlockFace) : MSBlock {
        val blockData = data as BlockData
        if (blockData !is org.bukkit.block.data.Directional)
            return this
        blockData.facing = facing
        return MSBlock(type, blockData, facing)
    }

    fun isSimilar(block: Block) : Boolean {
        var similar = block.type == type
        if (data is Directional) {
            val otherBData = block.blockData
            if (otherBData !is Directional)
                return false
            similar = data.facing == otherBData.facing
        }
        if (data is Bisected) {
            val otherBData = block.blockData
            if (otherBData !is Bisected)
                return false
            similar = data.half == otherBData.half
        }
        return similar
    }


    companion object {
        fun deserialize (map : Map<String, Any>) : MSBlock {
            val data = map["data"]
            val type = Material.getMaterial(map["type"] as String)!!
            return MSBlock(
                type,
                Bukkit.createBlockData(data as String),
                BlockFace.valueOf(map["facing"] as String)
            )
        }

        fun fromBlock(block : Block) : MSBlock {
            val data = block.blockData
            val face = if (block.blockData is org.bukkit.block.data.Directional) {
                val dir = block.blockData as org.bukkit.block.data.Directional
                dir.facing
            } else {
                BlockFace.SELF
            }
            return MSBlock(block.type, data, face)
        }
    }

    override fun toString(): String {
        return "(${type.name}, $data, $facing)"
    }
}