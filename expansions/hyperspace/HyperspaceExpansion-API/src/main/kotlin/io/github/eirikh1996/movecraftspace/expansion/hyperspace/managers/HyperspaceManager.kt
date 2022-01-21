package io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.HyperspaceBeacon
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.HyperspaceTravelEntry
import io.github.eirikh1996.movecraftspace.utils.MSUtils.plugin
import net.countercraft.movecraft.MovecraftLocation
import net.countercraft.movecraft.craft.Craft
import org.bukkit.*
import org.bukkit.boss.BossBar
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.BlockVector
import java.io.File
import java.lang.NumberFormatException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

object HyperspaceManager : BukkitRunnable(), Listener {
    val runnableMap = HashMap<UUID, BukkitRunnable>()
    val beaconLocations = HashSet<HyperspaceBeacon>()
    val craftsWithinBeaconRange = HashSet<Craft>()
    val ex = ExpansionManager.getExpansion("HyperspaceExpansion")!!
    val dataFolder = ex.dataFolder
    private val file = File(dataFolder, "beacons.yml")
    private val hyperspaceLocationsFile = File(dataFolder, "hyperspaceLocations.yml")
    val targetLocations = HashMap<MovecraftLocation, Location>()
    val beaconRange = ex.config.getInt("Beacon range")
    val processor : HyperspaceProcessor<*>

    init {
        val clazz = Class.forName("io.github.eirikh1996.movecraftspace.expansion.hyperspace.Movecraft" + (if (Settings.IsMovecraft8) "8" else "7") + "HyperspaceProcessor")
        if (!HyperspaceProcessor::class.java.isAssignableFrom(clazz))
            throw IllegalStateException()
        processor = clazz.getConstructor(Plugin::class.java).newInstance(plugin) as HyperspaceProcessor<*>
        Bukkit.getPluginManager().registerEvents(processor, plugin)
    }


    /**
     * When an object implementing interface `Runnable` is used
     * to create a thread, starting the thread causes the object's
     * `run` method to be called in that separately executing
     * thread.
     *
     *
     * The general contract of the method `run` is that it may
     * take any action whatsoever.
     *
     * @see java.lang.Thread.run
     */
    override fun run() {
        processor.processHyperspaceTravel()
        processTargetLocations()
    }






    private fun processTargetLocations() {
        val yml = YamlConfiguration()
        for (loc in targetLocations) {
            yml.set(loc.key.toString(), loc.value)
        }
        yml.save(hyperspaceLocationsFile)
    }

    private fun movecraftLocationFromString(str: String) : MovecraftLocation {
        val coords = str.replace("(", "").replace(")", "").split(",")
        try {
            return MovecraftLocation(coords[0].toInt(), coords[1].toInt(), coords[2].toInt());
        } catch (e : NumberFormatException) {
            throw IllegalArgumentException("String does not represent the toString() output of a MovecraftLocation", e)
        }
    }








    fun loadFile() {
        if (file.exists()) {
            val config = YamlConfiguration()
            config.load(file)
            val list = config.getMapList("beacons") as List<Map<String, Map<String, Any>>>
            for (map in list) {
                val originData = map["origin"]
                val destinationData = map["destination"]
                beaconLocations.add(HyperspaceBeacon(originData!!["name"] as String, Location.deserialize(originData["location"] as MutableMap<String, Any>),destinationData!!["name"] as String, Location.deserialize(destinationData["location"]!! as Map<String, Any>)))
            }
        }
         if (hyperspaceLocationsFile.exists()) {
             val yml = YamlConfiguration()
             yml.load(hyperspaceLocationsFile)
             yml.getKeys(true).forEach { key -> targetLocations[movecraftLocationFromString(key)] =
                 yml.getSerializable(key, Location::class.java)!! }
         }


    }

    fun saveFile() {
        if (!file.exists())
            file.createNewFile()
        val config = YamlConfiguration()
        val locationsToConfig = ArrayList<Map<String, Map<String, Any>>>()
        for (beacon in beaconLocations) {
            val entry = HashMap<String, Map<String, Any>>()
            val originData = HashMap<String, Any>()
            originData.put("name", beacon.originName)
            originData.put("location", beacon.origin.serialize())
            val destinationData = HashMap<String, Any>()
            destinationData.put("name", beacon.destinationName)
            destinationData.put("location", beacon.destination.serialize())
            entry.put("origin", originData)
            entry.put("destination", destinationData)
            locationsToConfig.add(entry)
        }
        config.set("beacons", locationsToConfig)
        config.save(file)
    }

    private val SHIFTS = arrayOf(
        BlockVector(0,1,0),
        BlockVector(0,-1,0),
        BlockVector(1,0,0),
        BlockVector(-1,0,0),
        BlockVector(0,0,1),
        BlockVector(0,0,-1)
    )

    abstract class HyperspaceProcessor<C> : Listener {
        protected val ex = ExpansionManager.getExpansion("HyperspaceExpansion")!!
        abstract val progressBars : MutableMap<C, BossBar>
        protected val craftsSunkInHyperspace = HashSet<C>()
        abstract fun scheduleHyperspaceTravel(craft : C, origin : Location, destination : Location, str : String = "", beaconTravel : Boolean = false)
        abstract fun processHyperspaceTravel()
        abstract fun pullOutOfHyperspace(entry: HyperspaceTravelEntry<C>, target : Location, exitMessage : String)
        abstract fun addEntry(craft: C, entry: HyperspaceTravelEntry<C>)
        abstract val pendingEntries : Map<C, HyperspaceTravelEntry<C>>
        abstract val processingEntries : WeakHashMap<C, HyperspaceTravelEntry<C>>
        abstract fun nearestUnobstructedLoc(loc: Location, craft: C): Location
    }
}