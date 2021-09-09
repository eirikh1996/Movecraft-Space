package io.github.eirikh1996.movecraftspace.expansion

import io.github.eirikh1996.movecraftspace.event.expansion.ExpansionDisableEvent
import io.github.eirikh1996.movecraftspace.event.expansion.ExpansionEnableEvent
import io.github.eirikh1996.movecraftspace.event.expansion.ExpansionLoadEvent
import io.github.eirikh1996.movecraftspace.objects.MSWorldBorder
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
    val description = classLoader.desc.getString("description", "")!!
    val dataFolder = File(classLoader.datafolder, name)
    val depend = HashSet<String>()
    val softdepend = HashSet<String>()
    val expansionDepend = HashSet<String>()
    val expansionSoftDepend = HashSet<String>()
    val commands = HashMap<String, Map<String, Any>>()
    val plugin = classLoader.plugin
    val logger = classLoader.plugin.logger
    val serverThread = Thread.currentThread()
    var state = ExpansionState.NOT_LOADED
    set(state) {
        field = state
        if (state == ExpansionState.ENABLED) {
            logMessage(LogMessageType.INFO, "Enabling expansion " + name)
            Bukkit.getPluginManager().callEvent(ExpansionEnableEvent(this))
            enable()
        } else if (state == ExpansionState.DISABLED) {
            logMessage(LogMessageType.INFO, "Disabling expansion " + name)
            Bukkit.getPluginManager().callEvent(ExpansionDisableEvent(this))
            disable()
        } else if (state == ExpansionState.LOADED) {
            logMessage(LogMessageType.INFO, "Loading expansion " + name)
            Bukkit.getPluginManager().callEvent(ExpansionLoadEvent(this))
            load()
        }
    }
    val version : String get() {
        return classLoader.desc.getString("version", "")!!
    }
    val uuid = UUID.randomUUID()

    init {
        depend.addAll(classLoader.desc.getStringList("depend"))
        softdepend.addAll(classLoader.desc.getStringList("softdepend"))
        expansionDepend.addAll(classLoader.desc.getStringList("expansiondepend"))
        expansionSoftDepend.addAll(classLoader.desc.getStringList("expansionsoftdepend"))
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
    open fun worldBoundrary(world : World) : MSWorldBorder {
        return MSWorldBorder.fromBukkit(world.worldBorder)
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

    fun logMessages(type: LogMessageType, messages : List<String>) {
        messages.forEach { s -> logMessage(type, s) }
    }

    fun logMessages(type: LogMessageType, vararg messages: String) {
        messages.forEach { s -> logMessage(type, s) }
    }

    fun logMessage(type : LogMessageType, message: String) {
        Bukkit.getConsoleSender().sendMessage(COMMAND_PREFIX + "§5[§3" + name + "§5]§r" + type.message + ": " + message)
    }

    fun saveDefaultConfig() {
        val configFile = File(dataFolder, "config.yml")
        if (!configFile.exists())
            saveResource("config.yml", false)
        config.load(configFile)
    }

    /**
     *
     * Saves a resource stored at the given path
     *
     * @param path String
     * @param replace Boolean
     * @param targetPath String
     */
    fun saveResource( path : String, replace : Boolean = false, targetPath : String = path.replace("\\", "/")) {
        if (path.length == 0) {
            return
        }
        val input = getResource(path.replace("\\", "/"))
        if (input == null) {
            logMessage(LogMessageType.CRITICAL,"Resource " + path.replace("\\", "/") + "does not exist")
            return
        }
        if (!dataFolder.exists())
            dataFolder.mkdirs()
        val file = File(dataFolder, targetPath.replace("\\", "/"))
        if (file.exists() && !replace) {
            logMessage(LogMessageType.CRITICAL,"Resource " + path.replace("\\", "/") + "already exists at target location")
            return
        }
        val lastIndex = targetPath.replace("\\", "/").lastIndexOf("/")
        val outDir = File(dataFolder.absolutePath, targetPath.replace("\\", "/").substring(0, if (lastIndex >= 0) lastIndex else 0))
        if (!outDir.exists())
            outDir.mkdirs()
        val buffer = ByteArray(1024)
        val output = FileOutputStream(file)
        var len : Int
        while ((input.read(buffer).also { len = it }) > 0) {
            output.write(buffer, 0, len)
        }
        input.close()
        output.close()
    }

    fun getResource(path : String) : InputStream? {
        val url = classLoader.getResource(path.replace("\\", "/"))
        if (url == null)
            return null
        val connection = url.openConnection()
        connection.useCaches = false
        return connection.getInputStream()
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
        INFO("INFO"), WARNING("§eWARNING"), ERROR("§cERROR"), CRITICAL("§4CRITICAL");

        override fun toString(): String {
            return message
        }
    }
}