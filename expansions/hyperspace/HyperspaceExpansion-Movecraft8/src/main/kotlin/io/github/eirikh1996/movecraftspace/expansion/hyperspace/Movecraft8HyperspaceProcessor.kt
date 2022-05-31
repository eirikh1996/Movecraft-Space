package io.github.eirikh1996.movecraftspace.expansion.hyperspace

import io.github.eirikh1996.movecraftspace.ConfigHolder.config
import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.ExpansionSettings.extraMassShadowRangeOfPlanets
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.ExpansionSettings.extraMassShadowRangeOfStars
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.ExpansionSettings.hypermatter
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.ExpansionSettings.hypermatterName
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.ExpansionSettings.hyperspaceChargeSound
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.ExpansionSettings.hyperspaceEnterSound
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.ExpansionSettings.hyperspaceWorld
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.events.HyperspaceChargeEvent
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.events.HyperspaceEnterEvent
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.events.HyperspaceExitEvent
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.events.HyperspaceTravelEvent
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.GravityWellManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.HyperdriveManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.HyperspaceManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.Movecraft8GravityWellProcessor
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.HyperspaceBeacon
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.HyperspaceTravelEntry
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.objects.StarCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils.plugin
import net.countercraft.movecraft.Movecraft
import net.countercraft.movecraft.MovecraftLocation
import net.countercraft.movecraft.craft.Craft
import net.countercraft.movecraft.craft.CraftManager
import net.countercraft.movecraft.craft.PilotedCraft
import net.countercraft.movecraft.craft.PlayerCraft
import net.countercraft.movecraft.craft.type.CraftType
import net.countercraft.movecraft.events.*
import net.countercraft.movecraft.libs.net.kyori.adventure.text.Component
import net.countercraft.movecraft.util.MathUtils
import net.countercraft.movecraft.util.hitboxes.BitmapHitBox
import net.countercraft.movecraft.util.hitboxes.HitBox
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.Sign
import org.bukkit.block.data.type.WallSign
import org.bukkit.boss.BarColor
import org.bukkit.boss.BossBar
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import java.lang.UnsupportedOperationException
import java.math.RoundingMode
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.*
import kotlin.random.Random

class Movecraft8HyperspaceProcessor(plugin: Plugin) : HyperspaceManager.HyperspaceProcessor<PlayerCraft>() {

    override val progressBars = HashMap<PlayerCraft, BossBar>()
    override val pendingEntries = WeakHashMap<PlayerCraft, HyperspaceTravelEntry<PlayerCraft>>()
    override val processingEntries = WeakHashMap<PlayerCraft, HyperspaceTravelEntry<PlayerCraft>>()

    override fun scheduleHyperspaceTravel(
        craft: PlayerCraft,
        origin: Location,
        destination: Location,
        str: String,
        beaconTravel: Boolean
    ) {
        val hyperdrivesOnCraft = HyperdriveManager.getHyperdrivesOnCraft(craft.hitBox.asSet(), craft.world)
        if (hyperdrivesOnCraft.isEmpty()) {
            craft.pilot.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "There are no hyperdrives on this craft")
            return
        }
        for (ml in craft.hitBox) {
            val testLoc = ml.toBukkit(craft.world)
            if (PlanetCollection.getPlanetAt(testLoc, extraMassShadowRangeOfPlanets) != null || StarCollection.getStarAt(testLoc, extraMassShadowRangeOfStars) != null || (GravityWellManager.processor as Movecraft8GravityWellProcessor).getActiveGravityWellAt(
                    testLoc,
                    craft
                ) != null) {
                craft.pilot.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Craft is within the range of a mass shadow. Move out of the mass shadow to initiate hyperspace jump")
                return
            }
        }
        if ((GravityWellManager.processor as Movecraft8GravityWellProcessor).craftHasActiveGravityWell(craft)) {
            craft.pilot.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Your craft cannot enter hyperspace because it has an active gravity well on it")
            return
        }




        if (HyperspaceManager.runnableMap.containsKey(craft.pilot.uniqueId)) {
            craft.pilot.sendMessage("You are already processing hyperspace warmup")
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
            craft.pilot.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "There is no hypermatter in any of the hyperdrives on the craft")
            return
        }
        warmupTime /= hyperdrivesWithHypermatter
        warmupTime /= hyperdrivesWithHypermatter
        if (craft.cruising)
            craft.cruising = false
        val hitBox =craft.hitBox
        warmupTime += (hitBox.size() * config.getDouble("Hyperspace warmup ship size multiplier", 0.0)).toInt()
        var dest = destination
        val buffer = if (hitBox.xLength > hitBox.zLength) {//Craft is facing east-west
            (hitBox.xLength / 2) + 1
        } else {//Craft is facing north-south
            (hitBox.zLength / 2) + 1
        }
        dest = ExpansionManager.worldBoundrary(dest.world!!).nearestLocWithin(dest, buffer)
        if (dest != destination) {
            craft.audience.sendMessage(Component.text().content(MSUtils.COMMAND_PREFIX + MSUtils.WARNING + "Destination location ${destination.toVector()} in world ${destination.world?.name} is outside of the world boundary. New adjusted target location is now ${dest.toVector()}").asComponent())
        }
        val entry = Movecraft8HyperspaceTravelEntry(craft, origin, dest, beaconTravel)
        entry.progressBar.addPlayer(craft.pilot)
        entry.progressBar.setTitle("$str Warmup 0.0%")
        val notifyP = craft.pilot
        val runnable = object : BukkitRunnable() {

            val timeStarted = System.currentTimeMillis()
            override fun run() {
                notifyP.playSound(notifyP.location, hyperspaceChargeSound, 0.1f, 0f)
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
                Bukkit.getScheduler().callSyncMethod(plugin) {
                    for (oentry in HyperdriveManager.getHyperdrivesOnCraft(craft.hitBox.asSet(), craft.world)) {
                        oentry.key.setLine(3, ChatColor.GOLD.toString() + "Travelling")
                        oentry.key.update()
                    }
                }

                notifyP.sendMessage("Initiating hyperspace jump")
                notifyP.playSound(notifyP.location, hyperspaceEnterSound, 15f, 0f)
                cancel()
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
                    hitboxObstructed = Bukkit.getScheduler().callSyncMethod(plugin) {
                        MSUtils.hitboxObstructed(
                            craft.hitBox.asSet(),
                            craft.type.getMaterialSetProperty(CraftType.PASSTHROUGH_BLOCKS),
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
                HyperspaceManager.runnableMap.remove(craft.pilot.uniqueId)?.cancel()
                HyperspaceManager.craftsWithinBeaconRange.remove(craft)
                craft.burningFuel += craft.type.getPerWorldProperty(CraftType.PER_WORLD_FUEL_BURN_RATE, craft.world) as Double
                craft.translate(hyperspaceWorld, dx, 0, dz)
                entry.lastTeleportTime = System.currentTimeMillis()
            }

        }
        HyperspaceManager.runnableMap[craft.pilot.uniqueId] = runnable
        runnable.runTaskTimerAsynchronously(plugin, 0, 1)
    }

    override fun processHyperspaceTravel() {
        for (entry in processingEntries.values) {
            if (System.currentTimeMillis() - entry.lastTeleportTime < 500 || entry.stage != HyperspaceTravelEntry.Stage.TRAVEL)
                continue
            val distance = entry.destination.toVector().clone().subtract(entry.origin.toVector().clone())
            val totalDistance = distance.length()
            val unit = distance.clone().normalize()
            unit.y = 0.0
            val speed = ex.config.getDouble("Hyperspace travel speed", 1.0)
            unit.multiply(speed)
            var massShadow = false
            val testLoc = entry.origin.clone().add(entry.progress)
            for (i in 1..speed.toInt()) {
                testLoc.add(unit.clone().normalize())
                HyperspaceManager.targetLocations[entry.craft.hitBox.midPoint] = testLoc
                val event = HyperspaceTravelEvent(entry.craft, testLoc)
                Bukkit.getScheduler().callSyncMethod(plugin) {
                    Bukkit.getPluginManager().callEvent(event)
                }.get()
                if (event.isCancelled) {
                    pullOutOfHyperspace(entry, event.currentLocation, event.exitMessage)
                    break
                }
                val foundGravityWell = Bukkit.getScheduler().callSyncMethod(plugin) {
                    (GravityWellManager.processor as Movecraft8GravityWellProcessor).getActiveGravityWellAt(
                        testLoc,
                        entry.craft
                    )
                }.get()
                if (PlanetCollection.getPlanetAt(testLoc, extraMassShadowRangeOfPlanets, true) != null || StarCollection.getStarAt(testLoc, extraMassShadowRangeOfStars) != null || foundGravityWell != null) {
                    pullOutOfHyperspace(
                        entry,
                        testLoc,
                        "Space craft caught in a mass shadow. Exiting hyperspace"
                    )
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
                var destination = entry.destination
                if (entry.beaconTravel) {
                    while (true) {
                        if (PlanetCollection.getPlanetAt(destination, 0, true) != null || StarCollection.getStarAt(destination) != null) {
                            val hitbox = entry.craft.hitBox
                            val bufferedRange = HyperspaceManager.beaconRange + max(hitbox.xLength, hitbox.zLength)
                            val perimeter = 2 * PI * HyperspaceManager.beaconRange
                            val arcLength = Random.nextDouble(0.0, perimeter)
                            val theta = arcLength / bufferedRange
                            destination = entry.destination.clone().add(cos(theta) * bufferedRange, 0.0, sin(theta) * bufferedRange)
                            continue
                        }
                        break
                    }
                }

                pullOutOfHyperspace(
                    entry,
                    destination,
                    "Space craft arived at destination. Exiting hyperspace"
                )
            }


        }
    }

    override fun pullOutOfHyperspace(entry: HyperspaceTravelEntry<PlayerCraft>, target : Location, exitMessage : String) {
        if (entry.stage == HyperspaceTravelEntry.Stage.FINISHED)
            return
        entry.stage = HyperspaceTravelEntry.Stage.FINISHED
        entry.craft.setProcessing(false)
        val midPoint = entry.craft.hitBox.midPoint
        entry.craft.pilot.sendMessage(exitMessage)
        entry.craft.burningFuel += entry.craft.type.getPerWorldProperty(CraftType.PER_WORLD_FUEL_BURN_RATE, entry.craft.world) as Double
        entry.progressBar.isVisible = false
        val finalTarget = nearestUnobstructedLoc(target, entry.craft)
        Bukkit.getScheduler().callSyncMethod(plugin) {
            for (hdentry in HyperdriveManager.getHyperdrivesOnCraft(entry.craft.hitBox.asSet(), entry.craft.world)) {
                hdentry.key.setLine(3, ChatColor.GOLD.toString() + "Standby")
                hdentry.key.update()
            }
        }.get()
        Bukkit.getPluginManager().callEvent(HyperspaceExitEvent(entry.craft))
        processingEntries.remove(entry.craft)
        entry.lastTeleportTime = System.currentTimeMillis()
        //Bukkit.broadcastMessage(finalTarget.toString())
        entry.craft.translate(entry.destination.world, finalTarget.blockX - midPoint.x, finalTarget.blockY - midPoint.y, finalTarget.blockZ - midPoint.z)
        entry.craft.pilot.playSound(entry.craft.pilot.location, ExpansionSettings.hyperspaceExitSound, 3f, 0f)
    }

    private fun processHyperspaceBeaconDetection(craft : PlayerCraft, hitBox: HitBox = craft.hitBox, world: World = craft.w) {
        var foundLoc : Location? = null
        var str = ""
        if (hitBox.isEmpty || craft.world == hyperspaceWorld)
            return
        for (beacon in HyperspaceManager.beaconLocations) {
            if (beacon.origin.world!! == world && beacon.origin.distance(hitBox.midPoint.toBukkit(world)) <= HyperspaceManager.beaconRange) {
                foundLoc = beacon.origin
                str = beacon.originName + "-" + beacon.destinationName
                break
            }
            if (beacon.destination.world!! == world && beacon.destination.distance(hitBox.midPoint.toBukkit(world)) <= HyperspaceManager.beaconRange) {
                foundLoc = beacon.destination
                str = beacon.destinationName + "-" + beacon.originName
                break
            }
        }
        if (foundLoc == null && HyperspaceManager.craftsWithinBeaconRange.contains(craft)) {
            craft.pilot.sendMessage("Exiting hyperspace beacon range")
            HyperspaceManager.craftsWithinBeaconRange.remove(craft)
            return
        }
        if (HyperspaceManager.runnableMap.containsKey(craft.pilot.uniqueId)) {
            HyperspaceManager.runnableMap.remove(craft.pilot.uniqueId)!!.cancel()
            craft.pilot.sendMessage(MSUtils.COMMAND_PREFIX + "Space craft was moved during warmup. Cancelling hyperspace warmup")
            var entry = processingEntries.remove(craft)
            if (entry == null)
                entry = pendingEntries.remove(craft)
            if (entry != null)
                entry.progressBar.isVisible = false
            progressBars.remove(craft)?.isVisible = false
        }
        if (foundLoc != null && !HyperspaceManager.craftsWithinBeaconRange.contains(craft)) {
            val clickText = TextComponent("§2[Accept]")
            clickText.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hyperspace travel")
            craft.pilot.spigot().sendMessage(
                TextComponent("Entered the range of $str hyperspace beacon "),
                clickText
            )
            HyperspaceManager.craftsWithinBeaconRange.add(craft)
        }
    }

    override fun nearestUnobstructedLoc(loc : Location, craft: PlayerCraft) : Location {
        if (loc.world == null)
            throw UnsupportedOperationException("Supplied location cannot have a null world")
        val hitBox = BitmapHitBox(craft.hitBox)
        if (hitBox.isEmpty)
            return loc
        val minHeight = min(max(craft.type.getPerWorldProperty(CraftType.PER_WORLD_MIN_HEIGHT_LIMIT, loc.world!!) as Int, loc.world!!.minHeight), loc.world!!.minHeight) + (hitBox.yLength / 2)
        val maxHeight = max(min(craft.type.getPerWorldProperty(CraftType.PER_WORLD_MAX_HEIGHT_LIMIT, loc.world!!) as Int, loc.world!!.maxHeight), loc.world!!.maxHeight) - (hitBox.yLength / 2)
        var foundLoc = loc.clone()
        foundLoc.y = min(foundLoc.y, (foundLoc.world!!.maxHeight - 1 - craft.hitBox.yLength / 2).toDouble())
        foundLoc.y = max(foundLoc.y, (foundLoc.world!!.minHeight + 1 + craft.hitBox.yLength / 2).toDouble())
        val bound = ExpansionManager.worldBoundrary(loc.world!!)
        val extraLength = ((if (hitBox.xLength > hitBox.zLength) hitBox.xLength else hitBox.zLength) / 2) + 1
        foundLoc = bound.nearestLocWithin(foundLoc, extraLength)

        var planet = PlanetCollection.getPlanetAt(foundLoc, ExpansionSettings.extraMassShadowRangeOfPlanets + extraLength, true)
        var star = StarCollection.getStarAt(foundLoc, ExpansionSettings.extraMassShadowRangeOfStars + extraLength)
        var beacon = getBeaconAt(foundLoc)
        if (!foundLoc.block.type.isAir || planet != null || star != null || beacon != null) {
            while (!foundLoc.block.type.isAir || planet != null || star != null || beacon != null) {
                val randomCoords = MSUtils.randomCoords(
                    loc.blockX - 250, loc.blockX + 250,
                    minHeight, maxHeight,
                    loc.blockZ - 250, loc.blockZ + 250
                )
                foundLoc = Location(foundLoc.world, randomCoords[0].toDouble(), randomCoords[1].toDouble(), randomCoords[2].toDouble())
                planet = PlanetCollection.getPlanetAt(foundLoc, ExpansionSettings.extraMassShadowRangeOfPlanets + extraLength, true)
                star = StarCollection.getStarAt(foundLoc, ExpansionSettings.extraMassShadowRangeOfStars + extraLength)
                beacon = getBeaconAt(foundLoc)
                val displacement = MathUtils.bukkit2MovecraftLoc(foundLoc).subtract(craft.hitBox.midPoint)
                if (craft.hitBox.any { ml ->
                        val targ = ml.add(displacement).toBukkit(foundLoc.world)
                        !targ.block.type.isAir && !craft.type.getMaterialSetProperty(CraftType.PASSTHROUGH_BLOCKS).contains(targ.block.type) } )
                    continue
            }
        }
        return foundLoc
    }

    fun getBeaconAt(loc : Location) : HyperspaceBeacon? {
        return HyperspaceManager.beaconLocations.firstOrNull { beacon -> beacon.origin == loc || beacon.destination == loc }
    }

    override fun addEntry(craft: PlayerCraft, entry: HyperspaceTravelEntry<PlayerCraft>) {
        pendingEntries[craft] = entry
    }


    @EventHandler
    fun onRelease(event : CraftReleaseEvent) {
        val craft = event.craft
        if (craft !is PlayerCraft)
            return
        if (craft.world != hyperspaceWorld) {
            return
        }
        if (event.reason == CraftReleaseEvent.Reason.FORCE) {
            return
        }
        craft.pilot.sendMessage("Cannot release a craft in hyperspace")
        event.isCancelled = true
    }

    @EventHandler
    fun onRotate(event : CraftRotateEvent) {
        val craft = event.craft
        if (craft !is PlayerCraft)
            return
        if (processingEntries.containsKey(craft)) {
            event.failMessage = MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Cannot move ship while travelling in hyperspace"
            event.isCancelled = true
            return
        }
        processHyperspaceBeaconDetection(craft, event.newHitBox)
    }

    @EventHandler
    fun onTranslate(event : CraftTranslateEvent) {
        val craft = event.craft
        if (craft !is PlayerCraft)
            return
        if (craftsSunkInHyperspace.contains(craft)) {
            craftsSunkInHyperspace.remove(craft)
            object : BukkitRunnable() {
                override fun run() {
                    CraftManager.getInstance().sink(craft)
                }
            }.runTaskLater(plugin, 3)
        }
        if (processingEntries.containsKey(event.craft)) {
            event.failMessage = MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Cannot move ship while travelling in hyperspace"
            event.isCancelled = true
            return
        }
        val entry = pendingEntries[craft]
        if (entry != null && event.world == hyperspaceWorld) {
            pendingEntries.remove(craft)
            processingEntries[craft] = entry
        }
        processHyperspaceBeaconDetection(craft, event.newHitBox, event.world)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onDetect(event : CraftDetectEvent) {
        val craft = event.craft
        if (craft !is PlayerCraft)
            return
        val result = HyperdriveManager.onCraftDetect(craft.pilot, craft.type.getStringProperty(CraftType.NAME), craft.hitBox.asSet(), craft.w)
        if (result.isNotEmpty()) {
            event.failMessage = result
            event.isCancelled = true
            return
        }
        if (craft.w != hyperspaceWorld)
            return
        if (craft.type.getBoolProperty(CraftType.CRUISE_ON_PILOT)) {
            event.failMessage = "Cannot launch cruiseOnPilot crafts in hyperspace"
            event.isCancelled = true
            return
        }
        val midpoint = craft.hitBox.midPoint
        object : BukkitRunnable() {
            override fun run() {
                var target = HyperspaceManager.targetLocations[midpoint]
                if (target == null) {
                    craft.pilot.sendMessage("Could not find a target location for the craft trapped in hyperspace. Please contact an administrator")
                    return
                }
                target = nearestUnobstructedLoc(target, craft)
                val dx = target.blockX - midpoint.x
                val dy = target.blockY - midpoint.y
                val dz = target.blockZ - midpoint.z
                craft.pilot.sendMessage("Craft was detected in hyperspace. Pulling you out of hyperspace")
                craft.translate(target.world, dx, dy, dz)
                HyperspaceManager.targetLocations.remove(midpoint)
            }

        }.runTaskLaterAsynchronously(plugin, 10)

    }

    @EventHandler
    fun onSink(event : CraftSinkEvent) {
        val craft = event.craft
        if (craft !is PlayerCraft)
            return
        val notifyP = craft.pilot
        if (craft.world != hyperspaceWorld)
            return
        val target = HyperspaceManager.targetLocations[craft.hitBox.midPoint]
        event.isCancelled = true
        craftsSunkInHyperspace.add(craft)
        object : BukkitRunnable() {
            override fun run() {
                pullOutOfHyperspace(processingEntries[craft]!!, target!!, "Craft was sunk in hyperspace. Exiting hyperspace")
            }

        }.runTaskAsynchronously(plugin)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onQuit(event : PlayerQuitEvent) {
        val p = event.player
        val craft = CraftManager.getInstance().getCraftByPlayer(p) ?: return
        if (craft.world != hyperspaceWorld)
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

    @EventHandler
    fun onDetectHyperspaceSign(e : CraftDetectEvent) {
        val craft = e.craft
        var range = 0
        HyperdriveManager.getHyperdrivesOnCraft(craft.hitBox.asSet(), craft.world).forEach { (t, u) -> range += u.maxRange }
        for (ml in craft.hitBox) {
            val b = ml.toBukkit(craft.w).block
            if (!b.type.name.endsWith("WALL_SIGN"))
                continue
            val sign = b.state as Sign
            if (sign.getLine(0) != "§bHyperspace") {
                continue
            }
            sign.setLine(1, "§9Range: §6$range")
            sign.update()
        }
    }
    @EventHandler
    fun onInteract(e : PlayerInteractEvent) {
        if (e.action != Action.RIGHT_CLICK_BLOCK)
            return
        if (!e.clickedBlock!!.type.name.endsWith("SIGN") && !e.clickedBlock!!.type.name.equals("SIGN_POST")) {
            return
        }
        val sign = e.clickedBlock!!.state as Sign
        if (!sign.getLine(0).equals("§bHyperspace")) {
            return
        }
        if (!e.player.hasPermission("movecraftspace.hyperspace.sign")) {
            e.player.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "You don't have permission to use the Hyperspace sign")
            return
        }
        val craft = CraftManager.getInstance().getCraftByPlayer(e.player)
        if (craft == null) {
            e.player.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "You are not piloting a craft")
            return
        }
        if (!craft.hitBox.contains(MathUtils.bukkit2MovecraftLoc(sign.location))) {
            e.player.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Sign is not on a piloted craft")
            return
        }
        if (pendingEntries.containsKey(craft) || processingEntries.containsKey(craft)) {
            e.player.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Hyperspace travel is already processing")
            return
        }   
        for (pl in PlanetCollection) {
            if (pl.space == craft.world) {
                continue
            }
            e.player.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "You can only use hyperspace travel in space worlds")
            return
        }
        if (!ExpansionSettings.allowedCraftTypesForHyperspaceSign.contains(craft.type.getStringProperty(CraftType.NAME))) {
            e.player.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Craft type " + craft.type.getStringProperty(CraftType.NAME) + " is not allowed for hyperspace travel using sign")
            return
        }
        val hyperdrivesOnCraft = HyperdriveManager.getHyperdrivesOnCraft(craft.hitBox.asSet(), craft.world)
        if (hyperdrivesOnCraft.isEmpty()) {
            e.player.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "There are no hyperdrives on this craft")
            return
        }
        var range = 0
        for (entry in hyperdrivesOnCraft) {
            val hyperdrive = entry.value
            range += hyperdrive.maxRange
        }
        val face = if (Settings.IsLegacy) {
            val signData = sign.data as org.bukkit.material.Sign
            signData.facing
        } else {
            if (sign.blockData is WallSign) {
                val signData = sign.blockData as WallSign
                signData.facing
            } else {
                val signData = sign.blockData as org.bukkit.block.data.type.Sign
                signData.rotation
            }
        }
        val direction = face.oppositeFace.direction.clone()
        direction.multiply(range)
        val origin = sign.location
        val destination = sign.location.clone().add(direction)
        scheduleHyperspaceTravel(craft, origin, destination)
    }

    private fun checkBrokenBlocksOnHyperdrives(block : Block): Boolean {
        val craft = CraftManager.getInstance().getCraftsInWorld(block.world).firstOrNull { c -> c.hitBox.contains(MathUtils.bukkit2MovecraftLoc(block.location)) } ?: return false
        if (craft !is PilotedCraft)
            return false
        if (!HyperdriveManager.getHyperdrivesOnCraft(craft.hitBox.asSet(), craft.world).any { e -> e.value.getStructure(e.key).contains(block) }) {
            return false
        }
        val entry = HyperspaceManager.processor.processingEntries[craft] as Movecraft8HyperspaceTravelEntry? ?: HyperspaceManager.processor.pendingEntries[craft] as Movecraft8HyperspaceTravelEntry? ?: return false
        if (craft.world != hyperspaceWorld) {
            craft.pilot.sendMessage(MSUtils.COMMAND_PREFIX + "Block on hyperdrive was broken. Warmup cancelled")
        } else {
            val targetLocation = HyperspaceManager.targetLocations[craft.hitBox.midPoint] ?: return false
            object : BukkitRunnable() {
                override fun run() {
                    pullOutOfHyperspace(entry, targetLocation, "Block was broken on one of the craft's hyperdrives. Exiting hyperspace")
                }
            }.runTaskAsynchronously(plugin)
        }
        entry.progressBar.isVisible = false
        processingEntries.remove(craft)
        pendingEntries.remove(craft)
        HyperspaceManager.runnableMap[craft.pilot.uniqueId]?.cancel()
        return true
    }

}