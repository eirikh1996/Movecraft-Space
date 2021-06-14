package io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.HyperspaceExpansion
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.MSBlock
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import net.countercraft.movecraft.craft.CraftManager
import net.countercraft.movecraft.craft.CraftType
import org.bukkit.block.Block
import org.bukkit.block.Sign
import org.bukkit.block.data.type.WallSign
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

data class GravityWell(val name : String, val blocks : Map<ImmutableVector, MSBlock>, val range : Int, val allowedOnCraftTypes : Set<CraftType> = HashSet()) {

    private fun getStructure(sign: Sign) : List<Block> {
        val face = if (Settings.IsLegacy) {
            val signData = sign.data as org.bukkit.material.Sign
            signData.facing
        } else {
            val signData = sign.blockData as WallSign
            signData.facing
        }
        val angle = MSUtils.angleBetweenBlockFaces(face, blocks[ImmutableVector.ZERO]!!.facing)

        val blockList = ArrayList<Block>()
        for (vec in blocks.keys) {
            blockList.add(sign.location.add(vec.rotate(angle, ImmutableVector.ZERO).add(0, vec.y, 0).toLocation(sign.world)).block)
        }
        return blockList
    }

    fun save() {
        val gravityWellDir = File(HyperspaceExpansion.instance.dataFolder, "gravitywells")
        if (!gravityWellDir.exists())
            gravityWellDir.mkdirs()
        val gravityWellFile = File(gravityWellDir, name + ".yml")
        val yaml = YamlConfiguration()
        yaml.set("name", name)
        yaml.set("range", range)
        val mapList = ArrayList<Map<String, Any>>()
        for (loc in blocks.keys) {
            val map = HashMap<String, Any>()
            map.putAll(loc.serialize())
            map.putAll(blocks[loc]!!.serialize())
            mapList.add(map)

        }
        yaml.set("blocks", mapList)
        yaml.save(gravityWellFile)

    }

    override fun hashCode(): Int {
        return name.hashCode() * range.hashCode() * allowedOnCraftTypes.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is GravityWell)
            return false
        return other.name == name && other.range == range && other.allowedOnCraftTypes == allowedOnCraftTypes
    }

    companion object {
        fun loadFromFile(file: File) : GravityWell {
            val yaml = YamlConfiguration()
            yaml.load(file)
            val blocks = yaml.getMapList("blocks") as List<Map<String, Any>>
            val name = yaml.getString("name")!!
            val range = yaml.getInt("range")
            val blockMap = HashMap<ImmutableVector, MSBlock>()
            for (block in blocks) {
                blockMap.put(ImmutableVector.deserialize(block), MSBlock.deserialize(block))
            }
            val allowedOnCraftTypes = HashSet<CraftType>()
            if (yaml.contains("allowedOnCraftTypes")) {
                yaml.getStringList("allowedOnCraftTypes").forEach { s -> allowedOnCraftTypes.add(CraftManager.getInstance().getCraftTypeFromString(s)) }
            }
            return GravityWell(name, blockMap, range, allowedOnCraftTypes)
        }
    }
}