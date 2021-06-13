package io.github.eirikh1996.movecraftspace.objects

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.utils.BlockUtils
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.material.Directional

data class MSBlock(val type : Material, val data : Byte, val facing : BlockFace = BlockFace.SELF) : ConfigurationSerializable {

    override fun serialize(): MutableMap<String, Any> {
        val map = HashMap<String, Any>()
        map.put("type", type.name)
        map.put("data", data)
        map.put("facing", facing.name)
        return map;
    }

    fun rotate(facing : BlockFace) : MSBlock {
        return if (Settings.IsLegacy) {
            val materialData = type.getNewData(data)
            if (materialData !is Directional)
                return this
            materialData.setFacingDirection(facing)
            MSBlock(type, materialData.data, materialData.facing)
        } else {
            val blockData = BlockUtils.blockDataFromMaterialandLegacyData(type, data)
            if (blockData !is org.bukkit.block.data.Directional)
                return this
            blockData.facing = facing
            MSBlock(type, BlockUtils.byteDataFromBlockData(blockData), facing)
        }
    }


    companion object {
        fun deserialize (data : Map<String, Any>) : MSBlock {
            return MSBlock(
                Material.getMaterial(data["type"] as String)!!,
                data["data"].toString().toByte(),
                BlockFace.valueOf(data["facing"] as String)
            )
        }

        fun fromBlock(block : Block) : MSBlock {
            val face = if (Settings.IsLegacy) {
                if (block.state.data is Directional) {
                    val dir = block.state.data as Directional
                    dir.facing
                } else {
                    BlockFace.SELF
                }
            } else {
                if (block.blockData is org.bukkit.block.data.Directional) {
                    val dir = block.blockData as org.bukkit.block.data.Directional
                    dir.facing
                } else {
                    BlockFace.SELF
                }
            }
            return MSBlock(block.type, block.data, face)
        }
    }
}