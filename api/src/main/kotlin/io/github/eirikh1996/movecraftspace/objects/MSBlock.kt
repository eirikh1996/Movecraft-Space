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

data class MSBlock(val type : Material, val data : Any = if (Settings.IsLegacy) 0 else Bukkit.createBlockData(type), val facing : BlockFace = BlockFace.SELF) : ConfigurationSerializable {

    override fun serialize(): MutableMap<String, Any> {
        val map = HashMap<String, Any>()
        map.put("type", type.name)
        map.put("data", if (Settings.IsLegacy) data else (data as BlockData).asString)
        map.put("facing", facing.name)
        return map;
    }

    fun rotate(facing : BlockFace) : MSBlock {
        return if (Settings.IsLegacy) {
            val materialData = type.getNewData(data as Byte)
            if (materialData !is Directional)
                return this
            materialData.setFacingDirection(facing)
            MSBlock(type, materialData.data)
        } else {
            val blockData = data as BlockData
            if (blockData !is org.bukkit.block.data.Directional)
                return this
            blockData.facing = facing
            MSBlock(type, blockData, facing)
        }
    }

    fun isSimilar(block: Block) : Boolean {
        var similar = block.type == type
        if (Settings.IsLegacy) {
            similar = block.data == data
        } else {
            val bData = data as BlockData

            if (bData is Directional) {
                val otherBData = block.blockData
                if (otherBData !is Directional)
                    return false
                similar = bData.facing == otherBData.facing
            }
            if (bData is Bisected) {
                val otherBData = block.blockData
                if (otherBData !is Bisected)
                    return false
                similar = bData.half == otherBData.half
            }
        }
        return similar
    }


    companion object {
        fun deserialize (map : Map<String, Any>) : MSBlock {
            val data = map["data"]
            val type = Material.getMaterial(map["type"] as String)!!
            return MSBlock(
                type,
                if (Settings.IsLegacy)
                    data.toString().toByte()
                else if (!Settings.IsLegacy && data is Int)
                    BlockUtils.blockDataFromMaterialandLegacyData(type, data.toByte())
                else Bukkit.createBlockData(data as String),
                BlockFace.valueOf(map["facing"] as String)
            )
        }

        fun fromBlock(block : Block) : MSBlock {
            val data : Any
            val face = if (Settings.IsLegacy) {
                data = block.data
                if (block.state.data is Directional) {
                    val dir = block.state.data as Directional
                    dir.facing
                } else {
                    BlockFace.SELF
                }
            } else {
                data = block.blockData
                if (block.blockData is org.bukkit.block.data.Directional) {
                    val dir = block.blockData as org.bukkit.block.data.Directional
                    dir.facing
                } else {
                    BlockFace.SELF
                }
            }
            return MSBlock(block.type, data, face)
        }
    }

    override fun toString(): String {
        return "(${type.name}, $data, $facing)"
    }
}