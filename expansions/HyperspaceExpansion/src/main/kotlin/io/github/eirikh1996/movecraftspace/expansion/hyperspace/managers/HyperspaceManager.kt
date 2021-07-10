package io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.HyperspaceBeacon
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.HyperspaceExpansion
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.HyperspaceTravelEntry
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.events.HyperspaceChargeEvent
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.events.HyperspaceEnterEvent
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.events.HyperspaceExitEvent
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.events.HyperspaceTravelEvent
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.objects.StarCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import net.countercraft.movecraft.MovecraftLocation
import net.countercraft.movecraft.craft.Craft
import net.countercraft.movecraft.craft.CraftManager
import net.countercraft.movecraft.events.CraftDetectEvent
import net.countercraft.movecraft.events.CraftReleaseEvent
import net.countercraft.movecraft.events.CraftRotateEvent
import net.countercraft.movecraft.events.CraftTranslateEvent
import net.countercraft.movecraft.utils.HitBox
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.*
import org.bukkit.boss.BarColor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.lang.NumberFormatException
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
    private val hyperspaceLocationsFile = File(HyperspaceExpansion.instance.dataFolder, "hyperspaceLocations.yml")
    val targetLocations = HashMap<MovecraftLocation, Location>()


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
        removedFinishedEntries()
        processTargetLocations()
    }
    fun addEntry(craft: Craft, entry: HyperspaceTravelEntry) {
        pendingEntries[craft] = entry
    }

    private fun processHyperspaceTravel() {
        for (entry in processingEntries.values) {
            if (System.currentTimeMillis() - entry.lastTeleportTime < 5000 || entry.stage != HyperspaceTravelEntry.Stage.TRAVEL)
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
                targetLocations[entry.craft.hitBox.midPoint] = testLoc
                val testUnit = unit.clone()
                testUnit.x += i
                testUnit.z += i
                testLoc.add(testUnit)
                val event = HyperspaceTravelEvent(entry.craft, testLoc)
                Bukkit.getScheduler().callSyncMethod(HyperspaceExpansion.instance.plugin) {
                    Bukkit.getPluginManager().callEvent(event)
                }.get()
                if (event.isCancelled) {
                    pullOutOfHyperspace(entry, event.currentLocation, event.exitMessage)
                    break
                }
                val foundGravityWell = Bukkit.getScheduler().callSyncMethod(HyperspaceExpansion.instance.plugin) {
                    GravityWellManager.getActiveGravityWellAt(
                        testLoc,
                        entry.craft
                    )
                }.get()
                if (PlanetCollection.getPlanetAt(testLoc, HyperspaceExpansion.instance.extraMassShadowRangeOfPlanets) != null || StarCollection.getStarAt(testLoc, HyperspaceExpansion.instance.extraMassShadowRangeOfStars) != null || foundGravityWell != null) {
                    pullOutOfHyperspace(entry, testLoc, "Space craft caught in a mass shadow. Exiting hyperspace")
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
                pullOutOfHyperspace(entry, destination, "Space craft arived at destination. Exiting hyperspace")
            }


        }
    }

    private fun removedFinishedEntries() {
        for (c in processingEntries.keys) {
            val entry = processingEntries[c]
            if (entry == null || entry.stage != HyperspaceTravelEntry.Stage.FINISHED)
                continue
            processingEntries.remove(c)
        }
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

    private fun pullOutOfHyperspace(entry: HyperspaceTravelEntry, target : Location, exitMessage : String) {
        if (entry.stage == HyperspaceTravelEntry.Stage.FINISHED)
            return
        entry.stage = HyperspaceTravelEntry.Stage.FINISHED
        entry.craft.setProcessing(false)
        val hitbox = entry.craft.hitBox
        val coords = randomCoords(entry.craft.notificationPlayer!!, target,
            HyperspaceExpansion.instance.config.getInt("Beacon range", 200) + if (hitbox.xLength > hitbox.zLength) hitbox.xLength else hitbox.zLength ,entry.craft.hitBox.yLength)
        val midPoint = entry.craft.hitBox.midPoint
        entry.craft.notificationPlayer!!.sendMessage(exitMessage)
        entry.craft.burningFuel += entry.craft.type.getFuelBurnRate(entry.craft.w)
        entry.progressBar.isVisible = false
        val finalTarget = Location(target.world, coords[0].toDouble(), coords[1].toDouble(),
            coords[2].toDouble()
        )
        Bukkit.getScheduler().callSyncMethod(HyperspaceExpansion.instance.plugin) {
            for (hdentry in HyperdriveManager.getHyperdrivesOnCraft(entry.craft)) {
                hdentry.key.setLine(3, ChatColor.GOLD.toString() + "Standby")
                hdentry.key.update()
            }
        }.get()
        Bukkit.getPluginManager().callEvent(HyperspaceExitEvent(entry.craft))
        entry.lastTeleportTime = System.currentTimeMillis()
        entry.craft.translate(entry.destination.world, finalTarget.blockX - midPoint.x, finalTarget.blockY - midPoint.y, finalTarget.blockZ - midPoint.z)
        entry.craft.notificationPlayer!!.playSound(entry.craft.notificationPlayer!!.location, HyperspaceExpansion.instance.hyperspaceExitSound, 3f, 0f)
    }


    internal fun randomCoords(p : Player , center : Location, radius : Int, heightToIgnore : Int) : IntArray {
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
        if (entry != null && event.world == HyperspaceExpansion.instance.hyperspaceWorld) {
            processingEntries[event.craft] = entry
        }
        processHyperspaceBeaconDetection(event.craft, event.newHitBox, event.world)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onDetect(event : CraftDetectEvent) {
        val craft = event.craft
        if (craft.w != HyperspaceExpansion.instance.hyperspaceWorld)
            return
        if (craft.type.cruiseOnPilot) {
            event.failMessage = "Cannot launch cruiseOnPilot crafts in hyperspace"
            event.isCancelled
            return
        }
        val midpoint = craft.hitBox.midPoint
        val target = targetLocations[midpoint]
        if (target == null) {
            craft.notificationPlayer!!.sendMessage("Could not find a target location for the craft trapped in hyperspace. Please contact an administrator")
            return
        }
        val coords = randomCoords(craft.notificationPlayer!!, target, 300, craft.hitBox.yLength)
        val dx = coords[0] - midpoint.x
        val dy = coords[1] - midpoint.y
        val dz = coords[2] - midpoint.z
        object : BukkitRunnable() {
            override fun run() {
                craft.notificationPlayer!!.sendMessage("Craft was detected in hyperspace. Pulling you out of hyperspace")
                craft.translate(target.world, dx, dy, dz)
                targetLocations.remove(midpoint)
            }

        }.runTaskLaterAsynchronously(HyperspaceExpansion.instance.plugin, 10)

    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onQuit(event : PlayerQuitEvent) {
        val p = event.player
        val craft = CraftManager.getInstance().getCraftByPlayer(p) ?: return
        if (craft.w != HyperspaceExpansion.instance.hyperspaceWorld)
            return
        if (!processingEntries.containsKey(craft))
            return
        processingEntries.remove(craft)
    }


    fun processHyperspaceBeaconDetection(craft : Craft, hitBox: HitBox = craft.hitBox, world: World = craft.w) {
        var foundLoc : Location? = null
        var str = ""
        if (hitBox.isEmpty)
            return
        for (beacon in beaconLocations) {
            if (beacon.origin.world!! == world && beacon.origin.distance(hitBox.midPoint.toBukkit(world)) <= HyperspaceExpansion.instance.config.getInt("Beacon range")) {
                foundLoc = beacon.origin
                str = beacon.originName + "-" + beacon.destinationName
                break
            }
            if (beacon.destination.world!! == world && beacon.destination.distance(hitBox.midPoint.toBukkit(world)) <= HyperspaceExpansion.instance.config.getInt("Beacon range")) {
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
                TextComponent("Entered the range of $str hyperspace beacon "),
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
        if (GravityWellManager.craftHasActiveGravityWell(craft)) {
            craft.notificationPlayer!!.sendMessage(COMMAND_PREFIX + ERROR + "Your craft cannot enter hyperspace because it has an active gravity well on it")
            return
        }
        val hypermatter = HyperspaceExpansion.instance.hypermatter
        val hypermatterName = HyperspaceExpansion.instance.hypermatterName



        if (runnableMap.containsKey(craft.notificationPlayer!!.uniqueId)) {
            craft.notificationPlayer!!.sendMessage("You are already processing hyperspace warmup")
            return
        }
        var warmupTime = 0
        var hyperdrivesWithHypermatter = 0
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

            val stack =  if (hypermatterName.isEmpty())
                foundFuelContainer.inventory.getItem(foundFuelContainer.inventory.first(hypermatter))
                        else {
                            var index = -1
                            for (e in foundFuelContainer.inventory.all(hypermatter)) {
                                if (e.value.itemMeta!!.displayName != hypermatterName)
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
            warmupTime += entry.value.warmupTime
            hyperdrivesWithHypermatter++
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
        warmupTime /= hyperdrivesWithHypermatter
        warmupTime /= hyperdrivesWithHypermatter
        if (craft.cruising)
            craft.cruising = false
        val hitBox =craft.hitBox
        val entry = HyperspaceTravelEntry(craft, origin, destination, beaconTravel)
        entry.progressBar.addPlayer(craft.notificationPlayer!!)
        entry.progressBar.setTitle("$str Warmup 0.0%")
        val notifyP = craft.notificationPlayer!!
        val runnable = object : BukkitRunnable() {

            val timeStarted = System.currentTimeMillis()
            override fun run() {
                notifyP.playSound(notifyP.location, HyperspaceExpansion.instance.hyperspaceChargeSound, 0.1f, 0f)
                val timePassed = (System.currentTimeMillis() - timeStarted) / 1000
                val progress = min(timePassed.toDouble() / warmupTime.toDouble(), 1.0)
                val percent = progress * 100
                Bukkit.getPluginManager().callEvent(HyperspaceChargeEvent(craft, percent))
                entry.progressBar.setTitle(str + " Warmup " + percent.toBigDecimal().setScale(2, RoundingMode.UP) + "%")
                entry.progressBar.progress = progress
                if (timePassed <= warmupTime)
                    return
                val event = HyperspaceEnterEvent(craft)
                Bukkit.getPluginManager().callEvent(event)
                if (event.isCancelled)
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
                Bukkit.broadcastMessage(bounds.asList().toString())
                val minX = (bounds[0] + (hitBox.xLength / 2)) + 10
                val maxX = (bounds[1] - (hitBox.xLength / 2)) - 10
                val minY = (if (Settings.IsV1_17) hyperspaceWorld.minHeight else 0) + (hitBox.yLength / 2) + 10
                val maxY = hyperspaceWorld.maxHeight - (hitBox.yLength / 2) - 10
                val minZ = (bounds[2] + (hitBox.zLength / 2)) + 10
                val maxZ = (bounds[3] - (hitBox.zLength / 2)) - 10
                var x = Random.nextInt(minX, maxX)
                var y = Random.nextInt(minY, maxY)
                var z = Random.nextInt(minZ, maxZ)
                var hitboxObstructed = true
                val midpoint = hitBox.midPoint
                while (hitboxObstructed) {
                    hitboxObstructed = Bukkit.getScheduler().callSyncMethod(HyperspaceExpansion.instance.plugin) {
                        MSUtils.hitboxObstructed(
                            craft,
                            null,
                            hyperspaceWorld,
                            MovecraftLocation(x - midpoint.x, y - midpoint.y, z - midpoint.z)
                        )
                    }.get()
                    if (!ExpansionManager.allowedArea(notifyP, MovecraftLocation(x, y, z).toBukkit(hyperspaceWorld)))
                        continue
                    if (!hitboxObstructed)
                        break
                    x = Random.nextInt(minX, maxX)
                    y = Random.nextInt(minY, maxY)
                    z = Random.nextInt(minZ, maxZ)
                }
                entry.progressBar.progress = 0.0
                entry.progressBar.color = BarColor.BLUE
                entry.stage = HyperspaceTravelEntry.Stage.TRAVEL
                val dx = x - midpoint.x
                val dz = z - midpoint.z
                runnableMap.remove(craft.notificationPlayer!!.uniqueId)?.cancel()
                craftsWithinBeaconRange.remove(craft)
                craft.burningFuel += craft.type.getFuelBurnRate(craft.w)
                craft.translate(hyperspaceWorld, dx, 0, dz)
                entry.lastTeleportTime = System.currentTimeMillis()
                addEntry(craft, entry)
            }

        }
        runnableMap[craft.notificationPlayer!!.uniqueId] = runnable
        runnable.runTaskTimerAsynchronously(HyperspaceExpansion.instance.plugin, 0, 1)
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
}