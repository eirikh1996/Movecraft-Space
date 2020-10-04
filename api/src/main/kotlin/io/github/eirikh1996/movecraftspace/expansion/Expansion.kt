package io.github.eirikh1996.movecraftspace.expansion

import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
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

/**
 *
 * Base class for all Movecraft-Space expansions
 *
 * @property config YamlConfiguration
 * @property classLoader ExpansionClassLoader
 * @property name String?
 * @property dataFolder File
 * @property depend HashSet<String>
 * @property softdepend HashSet<String>
 * @property commands HashMap<String, Map<String, Any>>
 * @property plugin Plugin
 * @property logger Logger
 * @property state ExpansionState
 * @property uuid (java.util.UUID..java.util.UUID?)
 */
abstract class Expansion {
    val config = YamlConfiguration()
    val classLoader = javaClass.classLoader as ExpansionClassLoader
    val name = classLoader.desc.getString("name")!!
    val dataFolder = File(classLoader.datafolder, name)
    val depend = HashSet<String>()
    val softdepend = HashSet<String>()
    val expansionDepend = HashSet<String>()
    val expansionSoftDepend = HashSet<String>()
    val commands = HashMap<String, Map<String, Any>>()
    val plugin = classLoader.plugin
    val logger = classLoader.plugin.logger
    var state = ExpansionState.NOT_LOADED
    set(state) {
        field = state
        if (state == ExpansionState.ENABLED) {
            Bukkit.getConsoleSender().sendMessage(COMMAND_PREFIX + "Enabling expansion " + name)
            enable()
        } else if (state == ExpansionState.DISABLED) {
            Bukkit.getConsoleSender().sendMessage(COMMAND_PREFIX + "Disabling expansion " + name)
            disable()
        } else if (state == ExpansionState.LOADED) {
            Bukkit.getConsoleSender().sendMessage(COMMAND_PREFIX + "Loading expansion " + name)
            load()
        }
    }
    val version : String get() {
        return classLoader.desc.getString("version", plugin.description.version)!!
    }
    val uuid = UUID.randomUUID()

    init {
        depend.addAll(classLoader.desc.getStringList("depend"))
        softdepend.addAll(classLoader.desc.getStringList("softdepend"))
        expansionDepend.addAll(classLoader.desc.getStringList("expansiondepend"))
        expansionSoftDepend.addAll(classLoader.desc.getStringList("softdepend"))
        val cmds = classLoader.desc.getConfigurationSection("commands")
        if (cmds != null) {
            val cmdMap = cmds.getValues(false)
            for (k in cmdMap.keys) {
                if (k == null)
                    continue
                val section = cmds.getConfigurationSection(k)
                val commandDetails = HashMap<String, Any>()
                commandDetails.put("aliases", section!!.getStringList("aliases"))
                commandDetails.put("description", section.getString("description", "")!!)
                commandDetails.put("usage", section.getString("usage", "")!!)
                commandDetails.put("permission", section.getString("permission", "")!!)
                commandDetails.put("permission-message", section.getString("permission-message", "")!!)
                commands.put(k, commandDetails)
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

    /**
     *
     * Method that check if a ship can enter a certain area in either a planet or space world
     *
     * @param p the player to check if allowed or not
     * @param loc Where to check
     * @return true if player is allowed, false otherwise
     */
    open fun allowedArea(p : Player, loc : Location) : Boolean {
        return true
    }
    fun logMessage(type : LogMessageType, message: String) {
        Bukkit.getConsoleSender().sendMessage(COMMAND_PREFIX + "[" + name + "]" + type + ": " + message)
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
        if (!dataFolder.exists())
            dataFolder.mkdirs()
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
        return classLoader.getResourceAsStream(path)
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

    enum class LogMessageType(val message : String) {
        INFO("INFO"), WARNING("§eWARNING"), ERROR("§cERROR"), CRITICAL("§4CRITICAL")
    }
}