package io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects


import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.expansion.selection.Structure
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.MSBlock
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import org.bukkit.block.Block
import org.bukkit.block.Sign
import org.bukkit.block.data.type.WallSign
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class NavigationComputer(name : String, val maxRange: Int, val allowedOnCraftTypes : Set<String> = HashSet()) : Structure(name) {
    val ex : Expansion = ExpansionManager.getExpansion("HyperspaceExpansion")!!

    override fun save() {
        val navComputerDir = File(ex.dataFolder, "navigationcomputers")
        if (!navComputerDir.exists())
            navComputerDir.mkdirs()
        val navComputerFile = File(navComputerDir, name + ".yml")
        val yaml = YamlConfiguration()
        yaml.set("name", name)
        yaml.set("maxRange", maxRange)
        val mapList = ArrayList<Map<String, Any>>()
        for (loc in blocks.keys) {
            val map = HashMap<String, Any>()
            map.putAll(loc.serialize())
            map.putAll(blocks[loc]!!.serialize())
            mapList.add(map)

        }
        if (!allowedOnCraftTypes.isEmpty()) {
            yaml.set("allowedOnCraftTypes", allowedOnCraftTypes)
        }
        yaml.set("blocks", mapList)
        yaml.save(navComputerFile)
    }

    fun isStructure(sign: Sign): Boolean {
        val signData = sign.blockData as WallSign
        val face = signData.facing
        val angle = MSUtils.angleBetweenBlockFaces(face, blocks[ImmutableVector.ZERO]!!.facing)
        for (b in getStructure(sign)) {
            val vec = ImmutableVector.fromLocation(b.location.subtract(sign.location))
            val rotated = vec.rotate(angle, ImmutableVector.ZERO).add(0,vec.y,0)
            val ncBlock = blocks[rotated]
            if (ncBlock != null && ncBlock.type == b.type)
                continue
            return false
        }
        return true
    }

    fun getStructure(sign: Sign) : List<Block> {
        val signData = sign.blockData as WallSign
        val face = signData.facing
        val angle = MSUtils.angleBetweenBlockFaces(blocks[ImmutableVector.ZERO]!!.facing, face)

        val blockList = ArrayList<Block>()
        for (vec in blocks.keys) {
            blockList.add(sign.location.add(vec.rotate(angle, ImmutableVector.ZERO).add(0, vec.y, 0).toLocation(sign.world)).block)
        }
        return blockList
    }

    override fun equals(other: Any?): Boolean {
        if (other !is NavigationComputer)
            return false
        return name == other.name && maxRange == other.maxRange && allowedOnCraftTypes == other.allowedOnCraftTypes
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    companion object {
        fun loadFromFile(file: File) : NavigationComputer {
            val yaml = YamlConfiguration()
            yaml.load(file)
            return deserialize(yaml.getValues(true))
        }

        fun deserialize(data : Map<String, Any>) : NavigationComputer {
            val blocks = data["blocks"] as List<Map<String, Any>>
            val name = data["name"] as String
            val maxRange = data["maxRange"] as Int
            val blockMap = HashMap<ImmutableVector, MSBlock>()
            for (block in blocks) {
                blockMap[ImmutableVector.deserialize(block)] = MSBlock.deserialize(block)
            }
            val allowedOnCraftTypes = ((data["allowedOnCraftTypes"] as Iterable<String>?)?:ArrayList()).toSet()
            val nc = NavigationComputer(name, maxRange, allowedOnCraftTypes)
            nc.blocks = blockMap
            return nc
        }
    }
}