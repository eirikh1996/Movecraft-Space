package io.github.eirikh1996.movecraftspace.expansion.hyperspace

import io.github.eirikh1996.movecraftspace.expansion.hyperspace.command.HyperspaceCommand
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.objects.StarCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import net.countercraft.movecraft.craft.Craft
import net.countercraft.movecraft.craft.CraftManager
import net.countercraft.movecraft.events.CraftReleaseEvent
import net.countercraft.movecraft.events.CraftRotateEvent
import net.countercraft.movecraft.events.CraftTranslateEvent
import net.countercraft.movecraft.utils.HitBox
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.math.RoundingMode
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.min
import kotlin.random.Random

object HyperspaceManager : BukkitRunnable(), Listener {
    private val pendingEntries = HashMap<Craft, HyperspaceTravelEntry>()
    private val processingEntries = HashMap<Craft, HyperspaceTravelEntry>()
    val beaconLocations = HashSet<HyperspaceBeacon>()
    val craftsWithinBeaconRange = HashSet<Craft>()
    private val file = File(HyperspaceExpansion.instance.dataFolder, "beacons.yml")
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
        processHyperspaceTravel()
    }
    fun addEntry(craft: Craft, entry: HyperspaceTravelEntry) {
        pendingEntries.put(craft, entry)
    }

    private fun processHyperspaceTravel() {
        val iterator = processingEntries.values.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val distance = entry.destination.toVector().clone().subtract(entry.origin.toVector().clone())
            val totalDistance = distance.length()
            val unit = distance.clone().normalize()
            val speed = HyperspaceExpansion.instance.config.getDouble("Hyperspace travel speed", 1.0)
            unit.x *= speed
            unit.y *= speed
            unit.z *= speed
            entry.progress.add(unit)
            val progress = min(entry.progress.length() / totalDistance, 1.0)
            val percent = progress * 100
            entry.progressBar.progress = progress
            val title = entry.progressBar.title.substring(0, entry.progressBar.title.indexOf(" "))
            entry.progressBar.setTitle(title + " Travel " + percent.toBigDecimal().setScale(2, RoundingMode.UP) + "%" )
            if (entry.progress.length() >= totalDistance) {
                val destination = entry.destination
                entry.craft.setProcessing(false)
                val hitbox = entry.craft.hitBox
                val coords = randomCoords(destination,HyperspaceExpansion.instance.config.getInt("Beacon range", 200) + if (hitbox.xLength > hitbox.zLength) hitbox.xLength else hitbox.zLength ,entry.craft.hitBox.yLength)
                val midPoint = entry.craft.hitBox.midPoint
                entry.craft.notificationPlayer!!.sendMessage("Space craft arrived at destination. Exiting hyperspace")
                entry.progressBar.isVisible = false
                entry.craft.translate(entry.destination.world, coords[0] - midPoint.x, coords[1] - midPoint.y, coords[2] - midPoint.z)
                iterator.remove()

                entry.craft.notificationPlayer!!.playSound(entry.craft.notificationPlayer!!.location, HyperspaceExpansion.instance.hyperspaceExitSound, 3f, 0f)
            }
            if (entry.origin.world != entry.destination.world)
                continue
            val testLoc = entry.origin.clone().add(entry.progress)
            if (PlanetCollection.getPlanetAt(testLoc) != null || StarCollection.getStarAt(testLoc) != null) {
                entry.craft.notificationPlayer!!.sendMessage("Space craft caught in a mass shadow. Exiting hyperspace")
                entry.craft.setProcessing(false)
                val coords = randomCoords(testLoc,500,entry.craft.hitBox.yLength)
                val midPoint = entry.craft.hitBox.midPoint
                entry.craft.translate(entry.destination.world, coords[0] - midPoint.x, coords[1] - midPoint.y, coords[2] - midPoint.z)
                entry.progressBar.isVisible = false
                processingEntries.remove(entry.craft)
            }

        }
    }

    private fun randomCoords(center : Location, radius : Int, heightToIgnore : Int) : IntArray {
        var coords = intArrayOf(
            Random.nextInt(center.blockX - radius, center.blockX + radius),
            Random.nextInt(heightToIgnore, 255 - heightToIgnore),
            Random.nextInt(center.blockZ - radius, center.blockZ + radius)
        )
        while (center.distance(Location(center.world, coords[0].toDouble(),
                coords[1].toDouble(), coords[2].toDouble()
            )) < 200) {
            coords = intArrayOf(
                Random.nextInt(center.blockX - radius, center.blockX + radius),
                Random.nextInt(heightToIgnore, 255 - heightToIgnore),
                Random.nextInt(center.blockZ - radius, center.blockZ + radius)
            )
        }
        return coords
    }

    @EventHandler
    fun onRelease(event : CraftReleaseEvent) {
        val craft = event.craft
        if (!craft.w.equals(HyperspaceExpansion.instance.hyperspaceWorld)) {
            return
        }
        if (event.reason == CraftReleaseEvent.Reason.FORCE) {
            return
        }
        craft.notificationPlayer!!.sendMessage("Cannot release a craft in hyperspace")
        event.isCancelled = true
    }

    @EventHandler
    fun onRotate(event : CraftRotateEvent) {
        if (processingEntries.containsKey(event.craft)) {
            event.failMessage = COMMAND_PREFIX + ERROR + "Cannot move ship while travelling in hyperspace"
            event.isCancelled = true
            return
        }
        processHyperspaceBeaconDetection(event.craft, event.newHitBox)
    }

    @EventHandler
    fun onTranslate(event : CraftTranslateEvent) {
        if (processingEntries.containsKey(event.craft)) {
            event.failMessage = COMMAND_PREFIX + ERROR + "Cannot move ship while travelling in hyperspace"
            event.isCancelled = true
            return
        }
        val entry = pendingEntries[event.craft]
        if (entry != null) {
            processingEntries.put(event.craft, entry)
        }
        processHyperspaceBeaconDetection(event.craft, event.newHitBox, event.world)
    }


    fun processHyperspaceBeaconDetection(craft : Craft, hitBox: HitBox = craft.hitBox, world: World = craft.w) {
        var foundLoc : Location? = null
        var str = ""
        if (hitBox.isEmpty)
            return
        for (beacon in beaconLocations) {
            if (beacon.origin.world!!.equals(world) && beacon.origin.distance(hitBox.midPoint.toBukkit(world)) <= HyperspaceExpansion.instance.config.getInt("Beacon range")) {
                foundLoc = beacon.origin
                str = beacon.originName + "-" + beacon.destinationName
                break
            }
            if (beacon.destination.world!!.equals(world) && beacon.destination.distance(hitBox.midPoint.toBukkit(world)) <= HyperspaceExpansion.instance.config.getInt("Beacon range")) {
                foundLoc = beacon.destination
                str = beacon.destinationName + "-" + beacon.originName
                break
            }
        }
        if (foundLoc == null && craftsWithinBeaconRange.contains(craft)) {
            craft.notificationPlayer!!.sendMessage("Exiting hyperspace beacon range")
            craftsWithinBeaconRange.remove(craft)
            return
        }
        if (HyperspaceCommand.runnableMap.containsKey(craft.notificationPlayer!!.uniqueId)) {
            HyperspaceCommand.runnableMap.remove(craft.notificationPlayer!!.uniqueId)!!.cancel()
            craft.notificationPlayer!!.sendMessage(COMMAND_PREFIX + "Space craft was moved during warmup. Cancelling hyperspace warmup")
            val entry = processingEntries.remove(craft)
            if (entry != null)
                entry.progressBar.isVisible = false
        }
        if (foundLoc != null && !craftsWithinBeaconRange.contains(craft)) {
            val clickText = TextComponent("§2[Accept]")
            clickText.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hyperspace travel")
            craft.notificationPlayer!!.spigot().sendMessage(
                TextComponent("Entered the range of " + str + " hyperspace beacon "),
                clickText
            )
            craftsWithinBeaconRange.add(craft)
        }
    }
    fun loadFile() {
        if (!file.exists())
            return
        val config = YamlConfiguration()
        config.load(file)
        val list = config.getMapList("beacons") as List<Map<String, Map<String, Any>>>
        for (map in list) {
            val originData = map["origin"]
            val destinationData = map["destination"]
            beaconLocations.add(HyperspaceBeacon(originData!!["name"] as String, Location.deserialize(originData["location"] as MutableMap<String, Any>),destinationData!!["name"] as String, Location.deserialize(destinationData["location"]!! as Map<String, Any>)))
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
}