package io.github.eirikh1996.movecraftspace.expansion

import com.google.common.collect.ImmutableMap
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginDescriptionFile
import java.io.File
import java.io.InputStreamReader
import java.util.jar.JarFile

object ExpansionManager {
    val expansions = HashSet<Expansion>()
    lateinit var pl: Plugin

    fun worldBoundrary(world: World) : IntArray {
        val lastBounds = intArrayOf(Int.MIN_VALUE, Int.MAX_VALUE, Int.MIN_VALUE, Int.MAX_VALUE)
        for (ex in getExpansions(ExpansionState.ENABLED)) {
            val bounds = ex.worldBoundrary(world)
            if (bounds[0] > lastBounds[0])
                lastBounds[0] = bounds[0]
            if (bounds[1] < lastBounds[1])
                lastBounds[1] = bounds[1]
            if (bounds[2] > lastBounds[2])
                lastBounds[2] = bounds[2]
            if (bounds[3] < lastBounds[3])
                lastBounds[3] = bounds[3]
        }
        return lastBounds
    }

    fun allowedArea(p : Player, loc : Location) : Boolean {
        for (ex in getExpansions(ExpansionState.ENABLED)) {
            val allowed = ex.allowedArea(p, loc)
            if (!allowed)
                return false
        }
        return true
    }

    fun getExpansions(state: ExpansionState) : Collection<Expansion> {
        return expansions.filter { ex -> ex.state == state }
    }

    fun loadExpansions() {
        if (!expansions.isEmpty())
            expansions.clear()
        val hookFolder = File(pl.dataFolder, "expansions")
        if (!hookFolder.exists())
            hookFolder.mkdirs()
        val jars = hookFolder.listFiles({ dir, name -> name.endsWith(".jar") })
        if (jars == null || jars.size == 0) {
            pl.logger.info("No expansions to load")
            return
        }

        for (jar in jars) {
            val jarFile = JarFile(jar)
            val yamlEntry = jarFile.getJarEntry("expansion.yml")
            if (yamlEntry == null) {
                pl.logger.severe("No expansion.yml found in " + jar.name)
                continue
            }
            val input = jarFile.getInputStream(yamlEntry)
            val reader = InputStreamReader(input)
            val desc = YamlConfiguration()
            desc.load(reader)
            val name = desc.getString("name")
            if (name == null) {
                pl.logger.severe("name is required, but not found in expansion.yml of " + jar.name)
                continue
            }
            val main = desc.getString("main")
            if (main == null) {
                pl.logger.severe("main is required, but not found in expansion.yml of " + jar.name)
                continue
            }
            val classLoader = ExpansionClassLoader(javaClass.classLoader, desc, jar.parentFile, jar, pl)
            val ex = classLoader.expansion

            val missingDependencies = HashSet<String>()
            val disabledDependencies = HashSet<String>()
            if (!missingDependencies.isEmpty()) {
                pl.logger.severe("Dependenc" + if (missingDependencies.size > 1 ) "ies " else "y " + missingDependencies.joinToString(", ") + if (missingDependencies.size > 1 ) " are " else " is " + "required, but missing")
                continue
            }
            if (!disabledDependencies.isEmpty()) {
                pl.logger.severe("Dependenc" + if (disabledDependencies.size > 1 ) "ies " else "y " + disabledDependencies.joinToString(", ") + if (disabledDependencies.size > 1 ) " are " else " is " + "required, but disabled")
                continue
            }
            val commandsField = PluginDescriptionFile::class.java.getDeclaredField("commands")
            commandsField.isAccessible = true
            val cmds = commandsField.get(pl.description) as Map<String, Map<String, Any>>
            val newCmds = HashMap<String, Map<String, Any>>(cmds)
            newCmds.putAll(ex.commands)
            commandsField.set(pl.description, ImmutableMap.copyOf(newCmds))
            try {
                ex.load()
                pl.logger.info("Expansion " + name + " loaded")
                expansions.add(ex)
            } catch (t : Throwable) {
                pl.logger.severe("Failure to load expansion " + ex.name)
                t.printStackTrace()
                continue
            }

        }
    }

    fun enableExpansions() {
        for (ex in expansions) {
            try {
                ex.enable()
                ex.state = ExpansionState.ENABLED
            } catch (t : Throwable) {
                pl.logger.severe("Failure to enable expansion " + ex.name)
                t.printStackTrace()
                ex.disable()
                ex.state = ExpansionState.DISABLED
            }

        }
    }

    fun disableExpansions() {
        for (ex in expansions) {
            ex.disable()
        }
    }
}