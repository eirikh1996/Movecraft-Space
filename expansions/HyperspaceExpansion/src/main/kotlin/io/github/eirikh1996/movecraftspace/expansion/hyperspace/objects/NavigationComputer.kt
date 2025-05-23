package io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects


import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.expansion.selection.Structure
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.MSBlock
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class NavigationComputer(name : String, val maxRange: Int, val allowedOnCraftTypes : Set<String> = HashSet()) : Structure(name) {
    val ex : Expansion = ExpansionManager.getExpansion("HyperspaceExpansion")!!

    override fun save() {
        val navComputerDir = File(ex.dataFolder, "hyperdrives")
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