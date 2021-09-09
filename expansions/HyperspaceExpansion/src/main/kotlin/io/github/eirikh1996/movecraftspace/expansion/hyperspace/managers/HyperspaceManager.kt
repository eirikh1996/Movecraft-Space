package io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.HyperspaceBeacon
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.HyperspaceExpansion
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.HyperspaceTravelEntry
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.events.hyperspace.HyperspaceChargeEvent
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.events.hyperspace.HyperspaceEnterEvent
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.events.hyperspace.HyperspaceExitEvent
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.events.hyperspace.HyperspaceTravelEvent
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.Hyperdrive
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.objects.StarCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import io.github.eirikh1996.movecraftspace.utils.MSUtils.WARNING
import net.countercraft.movecraft.MovecraftLocation
import net.countercraft.movecraft.craft.Craft
import net.countercraft.movecraft.craft.CraftManager
import net.countercraft.movecraft.events.*
import net.countercraft.movecraft.utils.BitmapHitBox
import net.countercraft.movecraft.utils.HitBox
import net.countercraft.movecraft.utils.MathUtils
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.Sign
import org.bukkit.boss.BarColor
import org.bukkit.boss.BossBar
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.BlockVector
import org.bukkit.util.Vector
import java.io.File
import java.lang.NumberFormatException
import java.lang.UnsupportedOperationException
import java.math.RoundingMode
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.*
import kotlin.random.Random

object HyperspaceManager : BukkitRunnable(), Listener {
    val runnableMap = HashMap<UUID, BukkitRunnable>()
    private val craftsSunkInHyperspace = HashSet<Craft>()
    private val pendingEntries = WeakHashMap<Craft, HyperspaceTravelEntry>()
    internal val processingEntries = WeakHashMap<Craft, HyperspaceTravelEntry>()
    val beaconLocations = HashSet<HyperspaceBeacon>()
    val craftsWithinBeaconRange = HashSet<Craft>()
    private val file = File(HyperspaceExpansion.instance.dataFolder, "beacons.yml")
    private val hyperspaceLocationsFile = File(HyperspaceExpansion.instance.dataFolder, "hyperspaceLocations.yml")
    val targetLocations = HashMap<MovecraftLocation, Location>()
    val beaconRange = HyperspaceExpansion.instance.config.getInt("Beacon range")
    internal val progressBars = HashMap<Craft, BossBar>()


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
        processTargetLocations()
    }
    fun addEntry(craft: Craft, entry: HyperspaceTravelEntry) {
        pendingEntries[craft] = entry
    }

    private fun processHyperspaceTravel() {
        for (entry in processingEntries.values) {
            if (System.currentTimeMillis() - entry.lastTeleportTime < 500 || entry.stage != HyperspaceTravelEntry.Stage.TRAVEL)
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
                testUnit.multiply(i)
                testLoc.add(testUnit)
                targetLocations[entry.craft.hitBox.midPoint] = testLoc
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
                if (PlanetCollection.getPlanetAt(testLoc, HyperspaceExpansion.instance.extraMassShadowRangeOfPlanets, true) != null || StarCollection.getStarAt(testLoc, HyperspaceExpansion.instance.extraMassShadowRangeOfStars) != null || foundGravityWell != null) {
                    pullOutOfHyperspace(entry, testLoc, "Space craft caught in a mass shadow. Exiting hyperspace")
                    massShadow = true
                    break
                }
            }
            if (massShadow)
                continue
            entry.progress.add(unit)
            val currentDistance = entry.progress.length()
            val progress = min(currentDistance / totalDistance, 1.0)
            val percent = progress * 100
            entry.progressBar.progress = progress
            val title = entry.progressBar.title.substring(0, entry.progressBar.title.indexOf(" "))
            entry.progressBar.setTitle(title + " Travel " + percent.toBigDecimal().setScale(2, RoundingMode.UP) + "%" )
            if (currentDistance >= totalDistance - 2) {
                val destination = entry.destination
                pullOutOfHyperspace(entry, destination, "Space craft arived at destination. Exiting hyperspace")
            }


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
        val midPoint = entry.craft.hitBox.midPoint
        entry.craft.notificationPlayer!!.sendMessage(exitMessage)
        entry.craft.burningFuel += entry.craft.type.getFuelBurnRate(entry.craft.w)
        entry.progressBar.isVisible = false
        val finalTarget = nearestUnobstructedLoc(target, entry.craft)
        Bukkit.getScheduler().callSyncMethod(HyperspaceExpansion.instance.plugin) {
            for (hdentry in HyperdriveManager.getHyperdrivesOnCraft(entry.craft)) {
                hdentry.key.setLine(3, ChatColor.GOLD.toString() + "Standby")
                hdentry.key.update()
            }
        }.get()
        Bukkit.getPluginManager().callEvent(HyperspaceExitEvent(entry.craft))
        processingEntries.remove(entry.craft)
        entry.lastTeleportTime = System.currentTimeMillis()
        entry.craft.translate(entry.destination.world, finalTarget.blockX - midPoint.x, finalTarget.blockY - midPoint.y, finalTarget.blockZ - midPoint.z)
        entry.craft.notificationPlayer!!.playSound(entry.craft.notificationPlayer!!.location, HyperspaceExpansion.instance.hyperspaceExitSound, 3f, 0f)
    }

    internal fun nearestUnobstructedLoc(loc : Location, craft: Craft) : Location {
        if (loc.world == null)
            throw UnsupportedOperationException("Supplied location cannot have a null world")
        val hitBox = BitmapHitBox(craft.hitBox)
        if (hitBox.isEmpty)
            return loc
        val queue = LinkedList<Location>()
        val minHeight = min(craft.type.getMinHeightLimit(loc.world!!), (if (Settings.IsV1_17) loc.world!!.minHeight else 0)) + (hitBox.yLength / 2)
        val maxHeight = max(craft.type.getMaxHeightLimit(loc.world!!), loc.world!!.maxHeight) - (hitBox.yLength / 2)
        var foundLoc = loc.clone()
        foundLoc.y = min(foundLoc.y, (foundLoc.world!!.maxHeight - 1 - craft.hitBox.yLength / 2).toDouble())
        foundLoc.y = max(foundLoc.y, ((if (Settings.IsV1_17) foundLoc.world!!.minHeight else 0) + 1 + craft.hitBox.yLength / 2).toDouble())
        val bound = ExpansionManager.worldBoundrary(loc.world!!)
        val extraLength = ((if (hitBox.xLength > hitBox.zLength) hitBox.xLength else hitBox.zLength) / 2) + 1
        foundLoc = bound.nearestLocWithin(foundLoc, extraLength * 2)
        beaconLocations.forEach { beacon ->
            val origin = beacon.origin
            val destination = beacon.destination
            val bufferedRange = beaconRange + (if (hitBox.xLength > hitBox.zLength) hitBox.xLength else hitBox.zLength)
            val closestLoc = when {
                origin.world == foundLoc.world && origin.distance(foundLoc) <= beaconRange -> origin
                destination.world == foundLoc.world && destination.distance(foundLoc) <= beaconRange -> destination
                else -> null
            }
            if (closestLoc != null) {
                val vector = closestLoc.clone().subtract(foundLoc).toVector()
                if (vector.length() > 0) { //Normalize distance vector if not a zero vector
                    vector.normalize()
                } else { //If it is a zero vector, due to closes loc being equal with target loc, get x and z fom the unit circle
                    val theta = Random.nextDouble(0.0, 2 * PI)
                    vector.x = cos(theta)
                    vector.z = sin(theta)
                }
                vector.y = 0.0
                vector.multiply(bufferedRange)
                foundLoc.add(vector)

            }
        }
        val planet = PlanetCollection.getPlanetAt(foundLoc, HyperspaceExpansion.instance.extraMassShadowRangeOfPlanets + extraLength, true)
        if (planet != null) {
            val bufferedRange = planet.radius + HyperspaceExpansion.instance.extraMassShadowRangeOfPlanets + 10 + (if (hitBox.xLength > hitBox.zLength) hitBox.xLength else hitBox.zLength)
            val foundLocVector = foundLoc.toVector()
            foundLocVector.y = planet.center.y.toDouble()
            val vector = planet.center.bukkitVector.subtract(foundLocVector)
            if (vector.length() > 0) { //Normalize distance vector if not a zero vector
                vector.normalize()
            } else { //If it is a zero vector, due to closes loc being equal with target loc, get x and z fom the unit circle
                val theta = Random.nextDouble(0.0, 2 * PI)
                vector.x = cos(theta)
                vector.z = sin(theta)
            }

            vector.multiply(bufferedRange)
            foundLoc.add(vector)
        }
        val star = StarCollection.getStarAt(foundLoc, HyperspaceExpansion.instance.extraMassShadowRangeOfStars + extraLength)
        if (star != null) {
            val bufferedRange = star.radius + HyperspaceExpansion.instance.extraMassShadowRangeOfPlanets + 10 + (if (hitBox.xLength > hitBox.zLength) hitBox.xLength else hitBox.zLength)
            val foundLocVector = foundLoc.toVector()
            foundLocVector.y = star.loc.y.toDouble()
            val vector = star.loc.bukkitVector.subtract(foundLocVector)
            if (vector.length() > 0) { //Normalize distance vector if not a zero vector
                vector.normalize()
            } else { //If it is a zero vector, due to closes loc being equal with target loc, get x and z fom the unit circle
                val theta = Random.nextDouble(0.0, 2 * PI)
                vector.x = cos(theta)
                vector.z = sin(theta)
            }

            vector.multiply(bufferedRange)
            foundLoc.add(vector)
        }
        queue.add(foundLoc)
        val visited = HashSet<Location>()
        while (!queue.isEmpty()) {
            val poll = queue.poll().clone()
            visited.add(poll)
            for (shift in SHIFTS) {
                val toAdd = poll.clone().add(shift)
                if (toAdd.blockY !in (minHeight + 1) until maxHeight || visited.contains(foundLoc))
                    continue
                queue.add(toAdd)
            }
            val centerPoint = hitBox.midPoint
            var clearArea = true



            hitBox.forEach { moveLoc ->
                val dx = moveLoc.x - centerPoint.x
                val dy = moveLoc.y - centerPoint.y
                val dz = moveLoc.z - centerPoint.z
                val foundBlock = poll.clone().add(dx.toDouble(), dy.toDouble(), dz.toDouble()).block
                if (!foundBlock.type.name.endsWith("AIR") && !craft.type.passthroughBlocks.contains(foundBlock.type)
                    || PlanetCollection.getPlanetAt(moveLoc.toBukkit(foundBlock.world) ) != null
                    || StarCollection.getStarAt(moveLoc.toBukkit(foundBlock.world) ) != null) {
                    clearArea = false
                    visited.add(moveLoc.toBukkit(foundBlock.world))
                }

            }
            if (!clearArea)
                continue

            foundLoc = poll
            break
        }
        return foundLoc
    }


    @EventHandler
    fun onRelease(event : CraftReleaseEvent) {
        val craft = event.craft
        if (craft.w != HyperspaceExpansion.instance.hyperspaceWorld) {
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
        if (craftsSunkInHyperspace.contains(event.craft)) {
            craftsSunkInHyperspace.remove(event.craft)
            object : BukkitRunnable() {
                override fun run() {
                    event.craft.sink()
                }
            }.runTaskLater(HyperspaceExpansion.instance.plugin, 3)
        }
        if (processingEntries.containsKey(event.craft)) {
            event.failMessage = COMMAND_PREFIX + ERROR + "Cannot move ship while travelling in hyperspace"
            event.isCancelled = true
            return
        }
        val entry = pendingEntries[event.craft]
        if (entry != null && event.world == HyperspaceExpansion.instance.hyperspaceWorld) {
            pendingEntries.remove(event.craft)
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
            event.isCancelled = true
            return
        }
        val midpoint = craft.hitBox.midPoint
        object : BukkitRunnable() {
            override fun run() {
                var target = targetLocations[midpoint]
                if (target == null) {
                    craft.notificationPlayer!!.sendMessage("Could not find a target location for the craft trapped in hyperspace. Please contact an administrator")
                    return
                }
                target = nearestUnobstructedLoc(target, craft)
                val dx = target.blockX - midpoint.x
                val dy = target.blockY - midpoint.y
                val dz = target.blockZ - midpoint.z
                craft.notificationPlayer!!.sendMessage("Craft was detected in hyperspace. Pulling you out of hyperspace")
                craft.translate(target.world, dx, dy, dz)
                targetLocations.remove(midpoint)
            }

        }.runTaskLaterAsynchronously(HyperspaceExpansion.instance.plugin, 10)

    }

    @EventHandler
    fun onSink(event : CraftSinkEvent) {
        val craft = event.craft
        val notifyP = craft.notificationPlayer
        if (craft.w != HyperspaceExpansion.instance.hyperspaceWorld)
            return
        val target = targetLocations[craft.hitBox.midPoint]
        event.isCancelled = true
        craft.sinking
        craftsSunkInHyperspace.add(craft)
        object : BukkitRunnable() {
            override fun run() {
                craft.notificationPlayer = notifyP
                pullOutOfHyperspace(processingEntries[craft]!!, target!!, "Craft was sunk in hyperspace. Exiting hyperspace")
            }

        }.runTaskAsynchronously(HyperspaceExpansion.instance.plugin)
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

    @EventHandler
    fun onBreak(event : BlockBreakEvent) {
        checkBrokenBlocksOnHyperdrives(event.block)
    }

    @EventHandler
    fun onEntityExplode(event : EntityExplodeEvent) {
        event.blockList().forEach { block ->
            if (checkBrokenBlocksOnHyperdrives(block))
                return
        }
    }

    @EventHandler
    fun onBlockExplode(event : BlockExplodeEvent) {
        event.blockList().forEach { block ->
            if (checkBrokenBlocksOnHyperdrives(block))
                return
        }
    }

    private fun checkBrokenBlocksOnHyperdrives(block : Block): Boolean {
        val craft = CraftManager.getInstance().getCraftsInWorld(block.world).firstOrNull { c -> c.hitBox.contains(MathUtils.bukkit2MovecraftLoc(block.location)) } ?: return false
        if (!HyperdriveManager.getHyperdrivesOnCraft(craft).any { e -> e.value.getStructure(e.key).contains(block) }) {
            return false
        }
        val entry = processingEntries[craft] ?: pendingEntries[craft] ?: return false
        if (craft.w != HyperspaceExpansion.instance.hyperspaceWorld) {
            craft.notificationPlayer?.sendMessage(COMMAND_PREFIX + "Block on hyperdrive was broken. Warmup cancelled")
        } else {
            val targetLocation = targetLocations[craft.hitBox.midPoint] ?: return false
            object : BukkitRunnable() {
                override fun run() {
                    pullOutOfHyperspace(entry, targetLocation, "Block was broken on one of the craft's hyperdrives. Exiting hyperspace")
                }
            }.runTaskAsynchronously(HyperspaceExpansion.instance.plugin)
        }
        entry.progressBar.isVisible = false
        processingEntries.remove(craft)
        pendingEntries.remove(craft)
        runnableMap[craft.notificationPlayer?.uniqueId]?.cancel()
        return true
    }


    private fun processHyperspaceBeaconDetection(craft : Craft, hitBox: HitBox = craft.hitBox, world: World = craft.w) {
        var foundLoc : Location? = null
        var str = ""
        if (hitBox.isEmpty || craft.w == HyperspaceExpansion.instance.hyperspaceWorld)
            return
        for (beacon in beaconLocations) {
            if (beacon.origin.world!! == world && beacon.origin.distance(hitBox.midPoint.toBukkit(world)) <= beaconRange) {
                foundLoc = beacon.origin
                str = beacon.originName + "-" + beacon.destinationName
                break
            }
            if (beacon.destination.world!! == world && beacon.destination.distance(hitBox.midPoint.toBukkit(world)) <= beaconRange) {
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
            var entry = processingEntries.remove(craft)
            if (entry == null)
                entry = pendingEntries.remove(craft)
            if (entry != null)
                entry.progressBar.isVisible = false
            progressBars.remove(craft)?.isVisible = false
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
        warmupTime += (hitBox.size() * HyperspaceExpansion.instance.config.getDouble("Hyperspace warmup ship size multiplier", 0.0)).toInt()
        var dest = destination
        dest = ExpansionManager.worldBoundrary(dest.world!!).nearestLocWithin(dest)
        if (dest != destination) {
            craft.notificationPlayer?.sendMessage(COMMAND_PREFIX + WARNING + "Destination location ${destination.toVector()} in world ${destination.world?.name} is outside of the world boundary. New adjusted target location is now ${dest.toVector()}")
        }
        val entry = HyperspaceTravelEntry(craft, origin, dest, beaconTravel)
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
                addEntry(craft, entry)
                if (timePassed <= warmupTime)
                    return
                val event = HyperspaceEnterEvent(craft)
                Bukkit.getPluginManager().callEvent(event)
                if (event.isCancelled)
                    return
                Bukkit.getScheduler().callSyncMethod(HyperspaceExpansion.instance.plugin) {
                    for (oentry in HyperdriveManager.getHyperdrivesOnCraft(craft)) {
                        oentry.key.setLine(3, ChatColor.GOLD.toString() + "Travelling")
                        oentry.key.update()
                    }
                }

                notifyP.sendMessage("Initiating hyperspace jump")
                notifyP.playSound(notifyP.location, HyperspaceExpansion.instance.hyperspaceEnterSound, 15f, 0f)
                cancel()
                val hyperspaceWorld = HyperspaceExpansion.instance.hyperspaceWorld
                val bounds = ExpansionManager.worldBoundrary(hyperspaceWorld)
                val minX = (bounds.minPoint.x + (hitBox.xLength / 2)) + 10
                val maxX = (bounds.maxPoint.x - (hitBox.xLength / 2)) - 10
                val minY = (if (Settings.IsV1_17) hyperspaceWorld.minHeight else 0) + (hitBox.yLength / 2) + 10
                val maxY = hyperspaceWorld.maxHeight - (hitBox.yLength / 2) - 10
                val minZ = (bounds.minPoint.z + (hitBox.zLength / 2)) + 10
                val maxZ = (bounds.maxPoint.z - (hitBox.zLength / 2)) - 10
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

    private val SHIFTS = arrayOf(
        BlockVector(0,1,0),
        BlockVector(0,-1,0),
        BlockVector(1,0,0),
        BlockVector(-1,0,0),
        BlockVector(0,0,1),
        BlockVector(0,0,-1)
    )
}