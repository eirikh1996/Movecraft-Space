package io.github.eirikh1996.movecraftspace.expansion

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import io.github.eirikh1996.movecraftspace.expansion.selection.SelectionManager
import io.github.eirikh1996.movecraftspace.expansion.selection.SelectionSupported
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.objects.StarCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import org.bukkit.Bukkit
import org.bukkit.Bukkit.getConsoleSender
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.InputStreamReader
import java.util.*
import java.util.Collections.unmodifiableSet
import java.util.jar.JarFile
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

object ExpansionManager : Iterable<Expansion> {
    val expansions = HashSet<Expansion>()
    val classCache = HashMap<String, Class<*>>()
    private var foundSelectionSupported = false
    val selectionsEnabled : Boolean get() = foundSelectionSupported
    lateinit var pl: JavaPlugin

    fun worldBoundrary(world: World) : IntArray {
        val lastBounds = intArrayOf(-29999984, 29999984, -29999984, 29999984)
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
        val coords = worldBoundrary(loc.world!!)
        if (coords[0] > loc.blockX || coords[1] < loc.blockX || coords[2] > loc.blockZ || coords[3] < loc.blockZ)
            return false
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
                getConsoleSender().sendMessage(COMMAND_PREFIX + ERROR + "No expansion.yml found in " + jar.name)
                continue
            }
            val input = jarFile.getInputStream(yamlEntry)
            val reader = InputStreamReader(input)
            val desc = YamlConfiguration()
            desc.load(reader)
            val name = desc.getString("name")
            if (name == null) {
                getConsoleSender().sendMessage(COMMAND_PREFIX + ERROR + "name is required, but not found in expansion.yml of " + jar.name)
                continue
            }
            val main = desc.getString("main")
            if (main == null) {
                getConsoleSender().sendMessage(COMMAND_PREFIX + ERROR + "main is required, but not found in expansion.yml of " + jar.name)
                continue
            }
            val classLoader : ExpansionClassLoader
            try {
                classLoader = ExpansionClassLoader(pl.javaClass.classLoader, desc, jar.parentFile, jar, pl)
            } catch (t : Throwable) {
                getConsoleSender().sendMessage(COMMAND_PREFIX + ERROR + "Cannot load expansion " + name)
                t.printStackTrace()
                continue
            }
            val ex = classLoader.expansion
            val softDependField = PluginDescriptionFile::class.java.getDeclaredField("softDepend")
            softDependField.isAccessible = true
            val softDependList = softDependField.get(pl.description) as List<String>
            val newSoftDependList = ArrayList(softDependList)
            newSoftDependList.addAll(ex.depend)
            newSoftDependList.addAll(ex.softdepend)
            softDependField.set(pl.description, ImmutableList.copyOf(newSoftDependList))
            val missingDependencies = ex.depend.filter { s -> Bukkit.getPluginManager().getPlugin(s) == null }
            if (!missingDependencies.isEmpty()) {
                ex.logMessage(Expansion.LogMessageType.ERROR, "Dependenc" + if (missingDependencies.size > 1 ) "ies " else "y " + missingDependencies.joinToString(", ") + if (missingDependencies.size > 1 ) " are " else " is " + "required, but missing")
                continue
            }
            val commandsField = PluginDescriptionFile::class.java.getDeclaredField("commands")
            commandsField.isAccessible = true
            val cmds = commandsField.get(pl.description) as Map<String, Map<String, Any>>
            val newCmds = HashMap<String, Map<String, Any>>(cmds)
            newCmds.putAll(ex.commands)
            commandsField.set(pl.description, ImmutableMap.copyOf(newCmds))
            expansions.add(ex)
            try {
                ex.state = ExpansionState.LOADED
                ex.logMessage(Expansion.LogMessageType.INFO , "Expansion " + name + " loaded")

            } catch (t : Throwable) {
                ex.logMessage(Expansion.LogMessageType.ERROR, "Failure to load expansion " + ex.name)
                t.printStackTrace()
                continue
            }

        }
    }

    fun enableExpansions() {
        getConsoleSender().sendMessage("§5=========[§9Movecraft-Space Expansions§5]=========§r ")
        getConsoleSender().sendMessage(COMMAND_PREFIX + "Enabling " + expansions.size + " loaded expansions")
        val softDependExpansions = HashSet<Expansion>()
        for (ex in expansions) {
            if (ex.expansionSoftDepend.any { s -> getExpansion(s) != null }) {
                softDependExpansions.add(ex)
                continue
            }
            try {
                ex.state = ExpansionState.ENABLED
                if (ex is SelectionSupported)
                    foundSelectionSupported = true
            } catch (t : Throwable) {
                ex.logMessage(Expansion.LogMessageType.ERROR, "Failure to enable expansion " + ex.name)
                t.printStackTrace()
                ex.state = ExpansionState.DISABLED
            }

        }
        for (ex in softDependExpansions) {
            try {
                ex.state = ExpansionState.ENABLED
            } catch (t : Throwable) {
                ex.logMessage(Expansion.LogMessageType.ERROR, "Failure to enable expansion " + ex.name)
                t.printStackTrace()
                ex.state = ExpansionState.DISABLED
            }
        }
        if (foundSelectionSupported) {
            getConsoleSender().sendMessage(COMMAND_PREFIX + "One or more expansions supporting selections have been found. Enabling selections")
            SelectionManager.pl = pl
            SelectionManager.initialize()
            Bukkit.getPluginManager().registerEvents(SelectionManager, pl)

        }
        getConsoleSender().sendMessage("§5=========[§9Movecraft-Space Expansions§5]=========§r ")
    }

    fun disableExpansions() {
        getConsoleSender().sendMessage("§5=========[§9Movecraft-Space Expansions§5]=========§r ")
        for (ex in expansions) {
            ex.state = ExpansionState.DISABLED
        }
        getConsoleSender().sendMessage("§5=========[§9Movecraft-Space Expansions§5]=========§r ")
    }

    /**
     * Returns an iterator over the elements of this object.
     */
    override fun iterator(): Iterator<Expansion> {
        return unmodifiableSet(expansions).iterator()
    }

    fun getExpansion(s: String): Expansion? {
        for (ex in this) {
            if (!ex.name.equals(s))
                continue
            return ex
        }
        return null
    }

    fun reloadExpansions() {
        disableExpansions()
        expansions.clear()
        loadExpansions()
        enableExpansions()
    }
}