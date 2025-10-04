package io.github.eirikh1996.expansion.asteroidbelt.`object`

import io.github.eirikh1996.expansion.asteroidbelt.AsteroidBeltExpansion
import io.github.eirikh1996.movecraftspace.expansion.selection.Structure
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.MSBlock
import net.countercraft.movecraft.MovecraftLocation
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class Asteroid(name : String) : Structure(name.lowercase()) {
    override fun save() {
        val yaml = YamlConfiguration()
        yaml.set("name", name)
        val blockList = ArrayList<Map<String, Any>>()
        blocks.forEach { (t, u) ->
            val blockLocs = HashMap<String, Any>()
            val locMap = HashMap<String, Any>()
            locMap["x"] = t.x
            locMap["y"] = t.y
            locMap["z"] = t.z
            blockLocs.putAll(locMap)
            blockLocs.putAll(u.serialize())
            blockList.add(blockLocs)
        }
        yaml.set("blocks", blockList)
        val file = File(AsteroidBeltExpansion.instance.dataFolder, "asteroids/${name.lowercase()}.yml")
        if (!file.exists())
            file.createNewFile()
        yaml.save(file)

    }

    companion object {
        fun loadFromFile(file: File) : Asteroid {
            val yaml = YamlConfiguration()
            yaml.load(file)
            return deserialize(yaml.getValues(true))
        }
        @JvmStatic fun deserialize(data : Map<String, Any>) : Asteroid {
            val blocks = data["blocks"] as List<Map<String, Any>>
            val name = data["name"] as String
            val blockMap = HashMap<MovecraftLocation, MSBlock>()
            for (block in blocks) {
                blockMap[MovecraftLocation(block["x"] as Int, block["y"] as Int, block["z"] as Int)] = MSBlock.deserialize(block)
            }

            val a =  Asteroid(name)
            a.blocks = blockMap
            return a
        }
    }
}