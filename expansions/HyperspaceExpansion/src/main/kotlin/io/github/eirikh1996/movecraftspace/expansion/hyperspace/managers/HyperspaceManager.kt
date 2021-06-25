package io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers

import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.HyperspaceBeacon
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.HyperspaceExpansion
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.HyperspaceTravelEntry
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.command.HyperspaceCommand
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.Hyperdrive
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.objects.StarCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import io.github.eirikh1996.movecraftspace.utils.MSUtils.WARNING
import net.countercraft.movecraft.MovecraftLocation
import net.countercraft.movecraft.craft.Craft
import net.countercraft.movecraft.events.CraftReleaseEvent
import net.countercraft.movecraft.events.CraftRotateEvent
import net.countercraft.movecraft.events.CraftTranslateEvent
import net.countercraft.movecraft.utils.HitBox
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.*
import org.bukkit.block.Sign
import org.bukkit.boss.BarColor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
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
    val runnableMap = HashMap<UUID, BukkitRunnable>()
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
            if (System.currentTimeMillis() - entry.lastTeleportTime < 5000)
                continue
            val distance = entry.destination.toVector().clone().subtract(entry.origin.toVector().clone())
            val totalDistance = distance.length()
            val unit = distance.clone().normalize()
            unit.y = 0.0
            val speed = HyperspaceExpansion.instance.config.getDouble("Hyperspace travel speed", 1.0)
            unit.x *= speed
            unit.z *= speed
            var massShadow = false
            for (i in 1..speed.toInt()) {
                val testLoc = entry.origin.clone().add(entry.progress)
                val testUnit = unit.clone()
                testUnit.x += i
                testUnit.z += i
                testLoc.add(testUnit)
                if (PlanetCollection.getPlanetAt(testLoc, HyperspaceExpansion.instance.extraMassShadowRangeOfPlanets) != null || StarCollection.getStarAt(testLoc, HyperspaceExpansion.instance.extraMassShadowRangeOfStars) != null || GravityWellManager.getActiveGravityWellAt(testLoc) != null) {
                    entry.craft.notificationPlayer!!.sendMessage("Space craft caught in a mass shadow. Exiting hyperspace")
                    entry.craft.setProcessing(false)
                    val coords = randomCoords(entry.craft.notificationPlayer!!, testLoc,500,entry.craft.hitBox.yLength)
                    val midPoint = entry.craft.hitBox.midPoint
                    Bukkit.getScheduler().callSyncMethod( HyperspaceExpansion.instance.plugin, {
                        for (hdentry in HyperdriveManager.getHyperdrivesOnCraft(entry.craft)) {
                            hdentry.key.setLine(3, ChatColor.GOLD.toString() + "Standby")
                            hdentry.key.update()
                        }
                    })

                    entry.craft.translate(entry.destination.world, coords[0] - midPoint.x, coords[1] - midPoint.y, coords[2] - midPoint.z)
                    entry.progressBar.isVisible = false
                    processingEntries.remove(entry.craft)
                    massShadow = true
                    break
                }
            }
            if (massShadow)
                continue
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
                val coords = randomCoords(entry.craft.notificationPlayer!!, destination,
                    HyperspaceExpansion.instance.config.getInt("Beacon range", 200) + if (hitbox.xLength > hitbox.zLength) hitbox.xLength else hitbox.zLength ,entry.craft.hitBox.yLength)
                val midPoint = entry.craft.hitBox.midPoint
                entry.craft.notificationPlayer!!.sendMessage("Space craft arrived at destination. Exiting hyperspace")
                entry.progressBar.isVisible = false
                Bukkit.getScheduler().callSyncMethod(HyperspaceExpansion.instance.plugin, {
                    for (hdentry in HyperdriveManager.getHyperdrivesOnCraft(entry.craft)) {
                        hdentry.key.setLine(3, ChatColor.GOLD.toString() + "Standby")
                        hdentry.key.update()
                    }
                })
                entry.craft.translate(entry.destination.world, coords[0] - midPoint.x, coords[1] - midPoint.y, coords[2] - midPoint.z)
                iterator.remove()
                processingEntries.remove(entry.craft)

                entry.craft.notificationPlayer!!.playSound(entry.craft.notificationPlayer!!.location, HyperspaceExpansion.instance.hyperspaceExitSound, 3f, 0f)
            }


        }
    }

    private fun randomCoords(p : Player , center : Location, radius : Int, heightToIgnore : Int) : IntArray {
        var coords = intArrayOf(
            Random.nextInt(center.blockX - radius, center.blockX + radius),
            Random.nextInt(heightToIgnore, 255 - heightToIgnore),
            Random.nextInt(center.blockZ - radius, center.blockZ + radius)
        )
        while (center.distance(Location(center.world, coords[0].toDouble(),
                coords[1].toDouble(), coords[2].toDouble()
            )) < 200) {
                val test = Location(center.world, coords[0].toDouble(), coords[1].toDouble(), coords[2].toDouble())
            if (ExpansionManager.allowedArea(p, test))
                continue
            if (StarCollection.getStarAt(test) != null || PlanetCollection.getPlanetAt(test) != null)
                continue
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
        val entry = pendingEntries.remove(event.craft)
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
        if (runnableMap.containsKey(craft.notificationPlayer!!.uniqueId)) {
            runnableMap.remove(craft.notificationPlayer!!.uniqueId)!!.cancel()
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

    fun scheduleHyperspaceTravel(craft: Craft, origin : Location, destination : Location, str : String = "", beaconTravel : Boolean = false) {


        val hyperdrivesOnCraft = HyperdriveManager.getHyperdrivesOnCraft(craft)
        if (hyperdrivesOnCraft.isEmpty()) {
            craft.notificationPlayer!!.sendMessage(COMMAND_PREFIX + ERROR + "There are no hyperdrives on this craft")
            return
        }
        for (ml in craft.hitBox) {
            val testLoc = ml.toBukkit(craft.w)
            if (PlanetCollection.getPlanetAt(testLoc, HyperspaceExpansion.instance.extraMassShadowRangeOfPlanets) != null || StarCollection.getStarAt(testLoc, HyperspaceExpansion.instance.extraMassShadowRangeOfStars) != null || GravityWellManager.getActiveGravityWellAt(testLoc, craft) != null) {
                craft.notificationPlayer!!.sendMessage(COMMAND_PREFIX + ERROR + "Craft is within the range of a mass shadow. Move out of the mass shadow to initiate hyperspace jump")
                return
            }
        }
        val hypermatter = HyperspaceExpansion.instance.hypermatter
        val hypermatterName = HyperspaceExpansion.instance.hypermatterName


        if (craft.cruising)
            craft.cruising = false
        if (runnableMap.containsKey(craft.notificationPlayer!!.uniqueId)) {
            craft.notificationPlayer!!.sendMessage("You are already processing hyperspace warmup")
            return
        }
        var foundHypermatter = false
        for (entry in hyperdrivesOnCraft) {
            val sign = entry.key
            val invBlocks = entry.value.getInventoryBlocks(sign)

            var foundFuelContainer : InventoryHolder? = null
            for (holder in invBlocks) {
                if (!holder.inventory.contains(hypermatter))
                    continue
                foundFuelContainer = holder
            }
            if (foundFuelContainer == null)
                continue

            val stack =  if (hypermatterName.length == 0)
                foundFuelContainer.inventory.getItem(foundFuelContainer.inventory.first(hypermatter))
                        else {
                            var index = -1
                            for (e in foundFuelContainer.inventory.all(hypermatter)) {
                                if (!e.value.itemMeta!!.displayName.equals(hypermatterName))
                                    continue
                                index = e.key
                            }
                if (index <= -1)
                    null
                else
                foundFuelContainer.inventory.getItem(index)
                        }


            if (stack == null)
                continue
            foundHypermatter = true
            if (stack.amount == 1) {
                foundFuelContainer.inventory.remove(stack)
            } else {
                stack.amount--
            }
            sign.setLine(3, ChatColor.GOLD.toString() + "Warming up")
            sign.update()
        }
        if (!foundHypermatter) {
            craft.notificationPlayer!!.sendMessage(COMMAND_PREFIX + ERROR + "There is no hypermatter in any of the hyperdrives on the craft")
            return
        }

        val hitBox =craft.hitBox
        val entry = HyperspaceTravelEntry(craft, origin, destination, beaconTravel)
        entry.progressBar.addPlayer(craft.notificationPlayer!!)
        entry.progressBar.setTitle(str + " Warmup 0.0%")
        val notifyP = craft.notificationPlayer!!
        val runnable = object : BukkitRunnable() {
            var warmupTime = 0
            init {
                hyperdrivesOnCraft.forEach { t, u -> warmupTime += u.warmupTime }
                warmupTime /= hyperdrivesOnCraft.size
                warmupTime /= hyperdrivesOnCraft.size
            }
            val timeStarted = System.currentTimeMillis()
            override fun run() {
                notifyP.playSound(notifyP.location, HyperspaceExpansion.instance.hyperspaceChargeSound, 0.1f, 0f)
                val timePassed = (System.currentTimeMillis() - timeStarted) / 1000
                val progress = min(timePassed.toDouble() / warmupTime.toDouble(), 1.0)
                val percent = progress * 100
                entry.progressBar.setTitle(str + " Warmup " + percent.toBigDecimal().setScale(2, RoundingMode.UP) + "%")
                entry.progressBar.progress = progress
                if (timePassed <= warmupTime)
                    return
                Bukkit.getScheduler().callSyncMethod(HyperspaceExpansion.instance.plugin) {
                    for (entry in HyperdriveManager.getHyperdrivesOnCraft(craft)) {
                        entry.key.setLine(3, ChatColor.GOLD.toString() + "Travelling")
                        entry.key.update()
                    }
                }

                notifyP.sendMessage("Initiating hyperspace jump")
                notifyP.playSound(notifyP.location, HyperspaceExpansion.instance.hyperspaceEnterSound, 15f, 0f)
                cancel()
                val hyperspaceWorld = HyperspaceExpansion.instance.hyperspaceWorld
                val bounds = ExpansionManager.worldBoundrary(hyperspaceWorld)
                val minX = (bounds[0] + (hitBox.xLength / 2)) + 10
                val maxX = (bounds[1] - (hitBox.xLength / 2)) - 10
                val minZ = (bounds[2] + (hitBox.zLength / 2)) + 10
                val maxZ = (bounds[3] - (hitBox.zLength / 2)) - 10
                var x = Random.nextInt(minX, maxX)
                var z = Random.nextInt(minZ, maxZ)
                var hitboxObstructed = true
                val midpoint = hitBox.midPoint
                while (hitboxObstructed) {
                    hitboxObstructed = Bukkit.getScheduler().callSyncMethod(HyperspaceExpansion.instance.plugin, {
                        MSUtils.hitboxObstructed(
                            craft,
                            null,
                            hyperspaceWorld,
                            MovecraftLocation(x - midpoint.x, 0, z - midpoint.z)
                        )
                    }).get()
                    if (!ExpansionManager.allowedArea(notifyP, MovecraftLocation(x - midpoint.x, 0, z - midpoint.z).toBukkit(hyperspaceWorld)))
                        continue
                    if (!hitboxObstructed)
                        break
                    x = Random.nextInt(minX, maxX)
                    z = Random.nextInt(minZ, maxZ)
                }
                entry.progressBar.progress = 0.0
                entry.progressBar.color = BarColor.BLUE
                val dx = x - midpoint.x
                val dz = z - midpoint.z
                val runnable = runnableMap.remove(craft.notificationPlayer!!.uniqueId)
                if (runnable != null) {
                    runnable.cancel()
                }
                craftsWithinBeaconRange.remove(craft)
                craft.burningFuel += craft.type.getFuelBurnRate(craft.w)
                craft.translate(hyperspaceWorld, dx, 0, dz)
                entry.lastTeleportTime = System.currentTimeMillis()
                addEntry(craft, entry)
            }

        }
        runnableMap.put(craft.notificationPlayer!!.uniqueId, runnable)
        runnable.runTaskTimerAsynchronously(HyperspaceExpansion.instance.plugin, 0, 1)
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