package io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.expansion.selection.Structure
import io.github.eirikh1996.movecraftspace.objects.MSBlock
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import org.bukkit.block.Block
import org.bukkit.block.Sign
import org.bukkit.block.data.type.WallSign
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.InventoryHolder
import java.io.File

class Hyperdrive(name : String, val warmupTime : Int, val allowedOnCraftTypes : Set<String> = HashSet()) : Structure(name) {

    fun getInventoryBlocks(sign: Sign) : List<InventoryHolder> {
        val inventoryBlocks = ArrayList<InventoryHolder>()
        for (b in getStructure(sign)) {
            if (b.state !is InventoryHolder)
                continue
            inventoryBlocks.add(b.state as InventoryHolder)
        }
        return inventoryBlocks
    }

    fun isStructure(sign: Sign): Boolean {
        val signData = sign.blockData as WallSign
        val face = signData.facing
        val angle = MSUtils.angleBetweenBlockFaces(face, blocks[ImmutableVector.ZERO]!!.facing)
        for (b in getStructure(sign)) {
            val vec = ImmutableVector.fromLocation(b.location.subtract(sign.location))
            val rotated = vec.rotate(angle, ImmutableVector.ZERO).add(0,vec.y,0)
            val hdBlock = blocks[rotated]
            if (hdBlock != null && hdBlock.type == b.type)
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

    override fun serialize(): MutableMap<String, Any> {
        val data = super.serialize()
        data["warmupTime"] = warmupTime
        data["allowedOnCraftTypes"] = allowedOnCraftTypes
        return data
    }

    override fun save() {
        val hyperdriveDir = File(ExpansionManager.getExpansion("HyperspaceExpansion")!!.dataFolder, "hyperdrives")
        if (!hyperdriveDir.exists())
            hyperdriveDir.mkdirs()
        val hyperdriveFile = File(hyperdriveDir, name + ".yml")
        val yaml = YamlConfiguration()
        yaml.set("name", name)
        yaml.set("warmupTime", warmupTime)
        val mapList = ArrayList<Map<String, Any>>()
        for (loc in blocks.keys) {
            val map = HashMap<String, Any>()
            map.putAll(loc.serialize())
            map.putAll(blocks[loc]!!.serialize())
            mapList.add(map)

        }
        if (!allowedOnCraftTypes.isEmpty()) {
            yaml.set("allowedOnCraftTypes", allowedOnCraftTypes.toList())
        }
        yaml.set("blocks", mapList)
        yaml.save(hyperdriveFile)

    }

    companion object {
        fun loadFromFile(file: File) : Hyperdrive {
            val yaml = YamlConfiguration()
            yaml.load(file)
            return deserialize(yaml.getValues(true))
        }
        @JvmStatic fun deserialize(data : Map<String, Any>) : Hyperdrive {
            val blocks = data["blocks"] as List<Map<String, Any>>
            val name = data["name"] as String
            val warmupTime = data.getOrDefault("warmupTime", 60) as Int
            val blockMap = HashMap<ImmutableVector, MSBlock>()
            for (block in blocks) {
                blockMap[ImmutableVector.deserialize(block)] = MSBlock.deserialize(block)
            }
            val allowedOnCraftTypes = ((data["allowedOnCraftTypes"] as Iterable<String>?)?:ArrayList()).toSet()

            val hd =  Hyperdrive(name, warmupTime, allowedOnCraftTypes)
            hd.blocks = blockMap
            return hd
        }
    }



}

