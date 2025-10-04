package io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects


import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.expansion.selection.Structure
import io.github.eirikh1996.movecraftspace.objects.MSBlock
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import net.countercraft.movecraft.MovecraftLocation
import net.countercraft.movecraft.util.MathUtils
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
            val locMap = HashMap<String, Any>()
            locMap["x"] = loc.x
            locMap["y"] = loc.y
            locMap["z"] = loc.z
            map.putAll(locMap)
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
        val angle = MSUtils.angleBetweenBlockFaces(face, blocks[zeroVector]!!.facing)
        for (b in getStructure(sign)) {
            val vec = MathUtils.bukkit2MovecraftLoc(b.location.subtract(sign.location))
            val rotated = rotate(angle, zeroVector, vec).add(MovecraftLocation(0,vec.y,0))
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
        val angle = MSUtils.angleBetweenBlockFaces(blocks[zeroVector]!!.facing, face)

        val blockList = ArrayList<Block>()
        for (vec in blocks.keys) {
            blockList.add(sign.location.add(rotate(angle, zeroVector, vec).add(MovecraftLocation(0, vec.y, 0)).toBukkit(sign.world)).block)
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
            val blockMap = HashMap<MovecraftLocation, MSBlock>()
            for (block in blocks) {
                blockMap[MovecraftLocation(block["x"] as Int, block["y"] as Int, block["z"] as Int)] = MSBlock.deserialize(block)
            }
            val allowedOnCraftTypes = ((data["allowedOnCraftTypes"] as Iterable<String>?)?:ArrayList()).toSet()
            val nc = NavigationComputer(name, maxRange, allowedOnCraftTypes)
            nc.blocks = blockMap
            return nc
        }
    }
}