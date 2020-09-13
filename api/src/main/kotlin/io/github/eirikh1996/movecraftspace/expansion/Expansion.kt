package io.github.eirikh1996.movecraftspace.expansion

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.min

abstract class Expansion {
    val config = YamlConfiguration()
    val classLoader = javaClass.classLoader as ExpansionClassLoader
    val name = classLoader.desc.getString("name")
    val dataFolder = classLoader.datafolder
    val depend = HashSet<String>()
    val softdepend = HashSet<String>()
    val commands = HashMap<String, Map<String, Any>>()
    val plugin = classLoader.plugin
    val logger = classLoader.plugin.logger
    var state = ExpansionState.NOT_LOADED
    set(state) {
        field = state
        if (state == ExpansionState.ENABLED)
            enable()
        else if (state == ExpansionState.DISABLED)
            disable()
    }
    val uuid = UUID.randomUUID()

    init {
        val tempDepend = classLoader.desc.getStringList("depend")
        if (tempDepend != null) {
            depend.addAll(tempDepend)
        }
        val tempSoftDepend = classLoader.desc.getStringList("softdepend")
        if (tempSoftDepend != null) {
            depend.addAll(tempSoftDepend)
        }
        val cmds = classLoader.desc.getConfigurationSection("commands")
        if (cmds != null) {
            val cmdMap = cmds.getValues(true)
            for (k in cmdMap.keys) {
                val v = cmdMap.get(k) as Map<String, Any>
                commands.put(k, v)
            }
        }
    }
    open fun worldBoundrary(world : World) : IntArray {
        val wb = world.worldBorder;
        val wbRadius = min((wb.size.toInt() / 2), 29999984)
        val minX = (wb.center.blockX - wbRadius)
        val maxX = (wb.center.blockX + wbRadius)
        val minZ = (wb.center.blockZ - wbRadius)
        val maxZ = (wb.center.blockZ + wbRadius)
        return intArrayOf(minX, maxX, minZ, maxZ)
    }
    open fun allowedArea(p : Player, loc : Location) : Boolean {
        return true
    }

    fun saveDefaultConfig() {
        saveResource("config.yml", false)
        config.load(File(dataFolder, "config.yml"))
    }
    fun saveResource(path : String, replace : Boolean) {
        if (path.length == 0) {
            return
        }
        val input = getResource(path)
        if (input == null) {
            Bukkit.getLogger().severe("[Movecraft-Space/Expansion] Resource " + path + "does not exist")
            return
        }
        val file = File(dataFolder, path)
        if (file.exists() && !replace) {
            Bukkit.getLogger().severe("[Movecraft-Space/Expansion] Resource " + path + "already exists at target location")
            return
        }
        val buffer = ByteArray(input.available())
        input.read(buffer)
        val output = FileOutputStream(file)
        output.write(buffer)
        input.close()
        output.close()
    }

    fun getResource(path : String) : InputStream? {
        return javaClass.classLoader.getResourceAsStream(path)
    }
    open fun load() {

    }

    open fun enable() {

    }

    open fun disable() {

    }

    override fun equals(other: Any?): Boolean {
        if (other !is Expansion)
            return false
        return other.uuid == uuid
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }
}