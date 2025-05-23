package io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers

import io.github.eirikh1996.movecraftspace.ConfigHolder
import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.ExpansionSettings
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.events.HyperspaceChargeEvent
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.events.HyperspaceEnterEvent
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.events.HyperspaceExitEvent
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.events.HyperspaceTravelEvent
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.HyperspaceBeacon
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.HyperspaceTravelEntry
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.objects.StarCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils.plugin
import net.countercraft.movecraft.MovecraftLocation
import net.countercraft.movecraft.craft.Craft
import net.countercraft.movecraft.craft.CraftManager
import net.countercraft.movecraft.craft.PilotedCraft
import net.countercraft.movecraft.craft.type.CraftType
import net.countercraft.movecraft.events.CraftReleaseEvent
import net.countercraft.movecraft.util.MathUtils
import net.countercraft.movecraft.util.hitboxes.HitBox
import net.countercraft.movecraft.util.hitboxes.BitmapHitBox
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.Sign
import org.bukkit.block.data.type.WallSign
import org.bukkit.boss.BarColor
import org.bukkit.boss.BossBar
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.BlockVector
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

    val progressBars = HashMap<Craft, BossBar>()
    val pendingEntries = WeakHashMap<Craft, HyperspaceTravelEntry>()
    val processingEntries = WeakHashMap<Craft, HyperspaceTravelEntry>()


    fun scheduleHyperspaceTravel(
        craft: PilotedCraft,
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
            if (PlanetCollection.getPlanetAt(testLoc,
                    ExpansionSettings.extraMassShadowRangeOfPlanets
                ) != null || StarCollection.getStarAt(testLoc,
                    ExpansionSettings.extraMassShadowRangeOfStars
                ) != null || (GravityWellManager.processor as Movecraft7GravityWellProcessor).getActiveGravityWellAt(
                    testLoc,
                    craft
                ) != null) {
                craft.pilot.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Craft is within the range of a mass shadow. Move out of the mass shadow to initiate hyperspace jump")
                return
            }
        }
        if ((GravityWellManager.processor as Movecraft7GravityWellProcessor).craftHasActiveGravityWell(craft)) {
            craft.pilot.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Your craft cannot enter hyperspace because it has an active gravity well on it")
            return
        }




        if (runnableMap.containsKey(craft.pilot.uniqueId)) {
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
                if (!holder.inventory.contains(ExpansionSettings.hypermatter))
                    continue
                foundFuelContainer = holder
            }
            if (foundFuelContainer == null)
                continue

            val stack =  if (ExpansionSettings.hypermatterName.isEmpty())
                foundFuelContainer.inventory.getItem(foundFuelContainer.inventory.first(ExpansionSettings.hypermatter))
            else {
                var index = -1
                for (e in foundFuelContainer.inventory.all(ExpansionSettings.hypermatter)) {
                    if (e.value.itemMeta!!.displayName != ExpansionSettings.hypermatterName)
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
        warmupTime += (hitBox.size() * ConfigHolder.config.getDouble("Hyperspace warmup ship size multiplier", 0.0)).toInt()
        var dest = destination
        val buffer = if (hitBox.xLength > hitBox.zLength) {//Craft is facing east-west
            (hitBox.xLength / 2) + 1
        } else {//Craft is facing north-south
            (hitBox.zLength / 2) + 1
        }
        dest = ExpansionManager.worldBoundrary(dest.world!!).nearestLocWithin(dest, buffer)
        if (dest != destination) {
            craft.pilot.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.WARNING + "Destination location ${destination.toVector()} in world ${destination.world?.name} is outside of the world boundary. New adjusted target location is now ${dest.toVector()}")
        }
        val entry = HyperspaceTravelEntry(craft, origin, dest, beaconTravel)
        entry.progressBar.addPlayer(craft.pilot)
        entry.progressBar.setTitle("$str Warmup 0.0%")
        val notifyP = craft.pilot
        val runnable = object : BukkitRunnable() {

            val timeStarted = System.currentTimeMillis()
            override fun run() {
                notifyP.playSound(notifyP.location, ExpansionSettings.hyperspaceChargeSound, 0.1f, 0f)
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
                Bukkit.getScheduler().callSyncMethod(MSUtils.plugin) {
                    for (oentry in HyperdriveManager.getHyperdrivesOnCraft(craft.hitBox.asSet(), craft.w)) {
                        oentry.key.setLine(3, ChatColor.GOLD.toString() + "Travelling")
                        oentry.key.update()
                    }
                }

                notifyP.sendMessage("Initiating hyperspace jump")
                notifyP.playSound(notifyP.location, ExpansionSettings.hyperspaceEnterSound, 15f, 0f)
                cancel()
                val bounds = ExpansionManager.worldBoundrary(ExpansionSettings.hyperspaceWorld)
                val minX = (bounds.minPoint.x + (hitBox.xLength / 2)) + 10
                val maxX = (bounds.maxPoint.x - (hitBox.xLength / 2)) - 10
                val minY = (if (Settings.IsV1_17) ExpansionSettings.hyperspaceWorld.minHeight else 0) + (hitBox.yLength / 2) + 10
                val maxY = ExpansionSettings.hyperspaceWorld.maxHeight - (hitBox.yLength / 2) - 10
                val minZ = (bounds.minPoint.z + (hitBox.zLength / 2)) + 10
                val maxZ = (bounds.maxPoint.z - (hitBox.zLength / 2)) - 10
                var x = Random.nextInt(minX, maxX)
                var y = Random.nextInt(minY, maxY)
                var z = Random.nextInt(minZ, maxZ)
                var hitboxObstructed = true
                val midpoint = hitBox.midPoint
                while (hitboxObstructed) {
                    hitboxObstructed = Bukkit.getScheduler().callSyncMethod(MSUtils.plugin) {
                        MSUtils.hitboxObstructed(
                            craft.hitBox.asSet(),
                            craft.type.getMaterialSetProperty(CraftType.PASSTHROUGH_BLOCKS),
                            null,
                            ExpansionSettings.hyperspaceWorld,
                            MovecraftLocation(x - midpoint.x, y - midpoint.y, z - midpoint.z)
                        )
                    }.get()
                    if (!ExpansionManager.allowedArea(notifyP, MovecraftLocation(x, y, z).toBukkit(ExpansionSettings.hyperspaceWorld)))
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
                runnableMap.remove(craft.pilot.uniqueId)?.cancel()
                craftsWithinBeaconRange.remove(craft)
                craft.burningFuel += craft.type.getPerWorldProperty(CraftType.PER_WORLD_FUEL_BURN_RATE, craft.world) as Double
                craft.translate(ExpansionSettings.hyperspaceWorld, dx, 0, dz)
                entry.lastTeleportTime = System.currentTimeMillis()
            }

        }
        HyperspaceManager.runnableMap[craft.pilot.uniqueId] = runnable
        runnable.runTaskTimerAsynchronously(MSUtils.plugin, 0, 1)
    }

    fun processHyperspaceTravel() {
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
                Bukkit.getScheduler().callSyncMethod(MSUtils.plugin) {
                    Bukkit.getPluginManager().callEvent(event)
                }.get()
                if (event.isCancelled) {
                    pullOutOfHyperspace(entry, event.currentLocation, event.exitMessage)
                    break
                }
                val foundGravityWell = Bukkit.getScheduler().callSyncMethod(MSUtils.plugin) {
                    (GravityWellManager.processor as Movecraft7GravityWellProcessor).getActiveGravityWellAt(
                        testLoc,
                        entry.craft
                    )
                }.get()
                if (PlanetCollection.getPlanetAt(testLoc,
                        ExpansionSettings.extraMassShadowRangeOfPlanets, true) != null || StarCollection.getStarAt(testLoc,
                        ExpansionSettings.extraMassShadowRangeOfStars
                    ) != null || foundGravityWell != null) {
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
                            val bufferedRange = beaconRange + max(hitbox.xLength, hitbox.zLength)
                            val perimeter = 2 * PI * beaconRange
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

    fun pullOutOfHyperspace(entry: HyperspaceTravelEntry, target : Location, exitMessage : String) {
        if (entry.stage == HyperspaceTravelEntry.Stage.FINISHED)
            return
        val craft = entry.craft
        entry.stage = HyperspaceTravelEntry.Stage.FINISHED
        entry.craft.setProcessing(false)
        val midPoint = entry.craft.hitBox.midPoint
        entry.craft.pilot.sendMessage(exitMessage)
        entry.craft.burningFuel += entry.craft.type.getPerWorldProperty(CraftType.PER_WORLD_FUEL_BURN_RATE, craft.world) as Double
        entry.progressBar.isVisible = false
        val finalTarget = nearestUnobstructedLoc(target, entry.craft)
        Bukkit.getScheduler().callSyncMethod(MSUtils.plugin) {
            for (hdentry in HyperdriveManager.getHyperdrivesOnCraft(entry.craft.hitBox.asSet(), entry.craft.world)) {
                hdentry.key.setLine(3, ChatColor.GOLD.toString() + "Standby")
                hdentry.key.update()
            }
        }.get()
        Bukkit.getPluginManager().callEvent(HyperspaceExitEvent(entry.craft))
        processingEntries.remove(entry.craft)
        entry.lastTeleportTime = System.currentTimeMillis()
        entry.craft.translate(entry.destination.world, finalTarget.blockX - midPoint.x, finalTarget.blockY - midPoint.y, finalTarget.blockZ - midPoint.z)
        entry.craft.pilot.playSound(entry.craft.pilot.location, ExpansionSettings.hyperspaceExitSound, 3f, 0f)
    }

    private fun processHyperspaceBeaconDetection(craft : PilotedCraft, hitBox: HitBox = craft.hitBox, world: World = craft.world) {
        var foundLoc : Location? = null
        var str = ""
        if (hitBox.isEmpty || craft.world == ExpansionSettings.hyperspaceWorld)
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
            craft.pilot.sendMessage("Exiting hyperspace beacon range")
            craftsWithinBeaconRange.remove(craft)
            return
        }
        if (runnableMap.containsKey(craft.pilot.uniqueId)) {
            runnableMap.remove(craft.pilot.uniqueId)!!.cancel()
            craft.pilot.sendMessage(MSUtils.COMMAND_PREFIX + "Space craft was moved during warmup. Cancelling hyperspace warmup")
            var entry = processingEntries.remove(craft)
            if (entry == null)
                entry = pendingEntries.remove(craft)
            if (entry != null)
                entry.progressBar.isVisible = false
            progressBars.remove(craft)?.isVisible = false
        }
        if (foundLoc != null && !craftsWithinBeaconRange.contains(craft)) {
            val text = Component.text("Entered the range of $str hyperspace beacon ")
                .append(Component.text().content("§2[Accept]")
                    .clickEvent(
                        ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/hyperspace travel")
                    ).build()
                )
            craft.pilot.sendMessage(text)
            craftsWithinBeaconRange.add(craft)
        }
    }

    fun nearestUnobstructedLoc(loc : Location, craft: Craft) : Location {
        if (loc.world == null)
            throw UnsupportedOperationException("Supplied location cannot have a null world")
        val hitBox = BitmapHitBox(craft.hitBox)
        if (hitBox.isEmpty)
            return loc
        val queue = LinkedList<Location>()
        val minHeight = min(craft.type.getPerWorldProperty(CraftType.PER_WORLD_MIN_HEIGHT_LIMIT, loc.world) as Int, loc.world.minHeight) + (hitBox.yLength / 2)
        val maxHeight = max(craft.type.getPerWorldProperty(CraftType.PER_WORLD_MAX_HEIGHT_LIMIT, loc.world) as Int, loc.world!!.maxHeight) - (hitBox.yLength / 2)
        var foundLoc = loc.clone()
        foundLoc.y = min(foundLoc.y, (foundLoc.world!!.maxHeight - 1 - craft.hitBox.yLength / 2).toDouble())
        foundLoc.y = max(foundLoc.y, ((if (Settings.IsV1_17) foundLoc.world!!.minHeight else 0) + 1 + craft.hitBox.yLength / 2).toDouble())
        val bound = ExpansionManager.worldBoundrary(loc.world!!)
        val extraLength = ((if (hitBox.xLength > hitBox.zLength) hitBox.xLength else hitBox.zLength) / 2) + 1
        foundLoc = bound.nearestLocWithin(foundLoc, extraLength * 2)

        var planet = PlanetCollection.getPlanetAt(foundLoc, ExpansionSettings.extraMassShadowRangeOfPlanets + extraLength, true)
        var star = StarCollection.getStarAt(foundLoc, ExpansionSettings.extraMassShadowRangeOfStars + extraLength)
        var beacon = getBeaconAt(foundLoc)
        if (!foundLoc.block.type.name.endsWith("AIR") || planet != null || star != null || beacon != null) {
            while (!foundLoc.block.type.name.endsWith("AIR") || planet != null || star != null || beacon != null) {
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
                        !targ.block.type.name.endsWith("AIR") && !craft.type.getMaterialSetProperty(CraftType.PASSTHROUGH_BLOCKS).contains(targ.block.type) } )
                    continue
            }
        }
        return foundLoc
    }

    fun getBeaconAt(loc : Location) : HyperspaceBeacon? {
        return HyperspaceManager.beaconLocations.firstOrNull { beacon -> beacon.origin == loc || beacon.destination == loc }
    }

    fun addEntry(craft: Craft, entry: HyperspaceTravelEntry) {
        pendingEntries[craft] = entry
    }


    @EventHandler
    fun onRelease(event : CraftReleaseEvent) {
        val craft = event.craft
        if (craft.w != ExpansionSettings.hyperspaceWorld) {
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
        if (processingEntries.containsKey(event.craft)) {
            event.failMessage = MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Cannot move ship while travelling in hyperspace"
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
            }.runTaskLater(MSUtils.plugin, 3)
        }
        if (processingEntries.containsKey(event.craft)) {
            event.failMessage = MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Cannot move ship while travelling in hyperspace"
            event.isCancelled = true
            return
        }
        val entry = pendingEntries[event.craft]
        if (entry != null && event.world == ExpansionSettings.hyperspaceWorld) {
            pendingEntries.remove(event.craft)
            processingEntries[event.craft] = entry
        }
        processHyperspaceBeaconDetection(event.craft, event.newHitBox, event.world)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onDetect(event : CraftDetectEvent) {
        val craft = event.craft
        val result = HyperdriveManager.onCraftDetect(craft.pilot, craft.type.craftName, craft.hitBox.asSet(), craft.w)
        if (result.isNotEmpty()) {
            event.failMessage = result
            event.isCancelled = true
            return
        }
        if (craft.w != ExpansionSettings.hyperspaceWorld)
            return
        if (craft.type.cruiseOnPilot) {
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

        }.runTaskLaterAsynchronously(MSUtils.plugin, 10)

    }

    @EventHandler
    fun onSink(event : CraftSinkEvent) {
        val craft = event.craft
        val notifyP = craft.notificationPlayer
        if (craft.w != ExpansionSettings.hyperspaceWorld)
            return
        val target = HyperspaceManager.targetLocations[craft.hitBox.midPoint]
        event.isCancelled = true
        craft.sinking
        craftsSunkInHyperspace.add(craft)
        object : BukkitRunnable() {
            override fun run() {
                craft.notificationPlayer = notifyP
                pullOutOfHyperspace(processingEntries[craft]!!, target!!, "Craft was sunk in hyperspace. Exiting hyperspace")
            }

        }.runTaskAsynchronously(MSUtils.plugin)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onQuit(event : PlayerQuitEvent) {
        val p = event.player
        val craft = CraftManager.getInstance().getCraftByPlayer(p) ?: return
        if (craft.w != ExpansionSettings.hyperspaceWorld)
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

    @EventHandler(priority = EventPriority.LOW)
    fun onDetectHyperspaceSign(e : CraftDetectEvent) {
        val craft = e.craft
        var range = 0
        HyperdriveManager.getHyperdrivesOnCraft(craft.hitBox.asSet(), craft.w).forEach { (t, u) -> range += u.maxRange }
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
        if (!e.clickedBlock!!.type.name.endsWith("SIGN") && e.clickedBlock!!.type.name != "SIGN_POST") {
            return
        }
        val sign = e.clickedBlock!!.state as Sign
        if (sign.getLine(0) != "§bHyperspace") {
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
            if (pl.space == craft.w) {
                continue
            }
            e.player.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "You can only use hyperspace travel in space worlds")
            return
        }
        if (!ExpansionSettings.allowedCraftTypesForHyperspaceSign.contains(craft.type.craftName)) {
            e.player.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Craft type " + craft.type.craftName + " is not allowed for hyperspace travel using sign")
            return
        }
        val hyperdrivesOnCraft = HyperdriveManager.getHyperdrivesOnCraft(craft.hitBox.asSet(), craft.w)
        if (hyperdrivesOnCraft.isEmpty()) {
            e.player.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "There are no hyperdrives on this craft")
            return
        }
        var range = 0
        for (entry in hyperdrivesOnCraft) {
            val hyperdrive = entry.value
            range += hyperdrive.maxRange
        }
        val face = if (sign.blockData is WallSign) {
            val signData = sign.blockData as WallSign
            signData.facing
        } else {
            val signData = sign.blockData as org.bukkit.block.data.type.Sign
            signData.rotation
        }
        val direction = face.oppositeFace.direction.clone()
        direction.multiply(range)
        val origin = sign.location
        val destination = sign.location.clone().add(direction)
        scheduleHyperspaceTravel(craft, origin, destination)
    }

    private fun checkBrokenBlocksOnHyperdrives(block : Block): Boolean {
        val craft = CraftManager.getInstance().getCraftsInWorld(block.world).firstOrNull { c -> c.hitBox.contains(
            MathUtils.bukkit2MovecraftLoc(block.location)) && c is PilotedCraft
        } ?: return false
        if (craft !is PilotedCraft)
            return false
        if (!HyperdriveManager.getHyperdrivesOnCraft(craft.hitBox.asSet(), craft.world).any { e -> e.value.getStructure(e.key).contains(block) }) {
            return false
        }
        val entry = HyperspaceManager.processor.processingEntries[craft] as Movecraft7HyperspaceTravelEntry? ?: HyperspaceManager.processor.pendingEntries[craft] as Movecraft7HyperspaceTravelEntry? ?: return false
        if (craft.world != ExpansionSettings.hyperspaceWorld) {
            craft.pilot.sendMessage(MSUtils.COMMAND_PREFIX + "Block on hyperdrive was broken. Warmup cancelled")
        } else {
            val targetLocation = HyperspaceManager.targetLocations[craft.hitBox.midPoint] ?: return false
            object : BukkitRunnable() {
                override fun run() {
                    pullOutOfHyperspace(entry, targetLocation, "Block was broken on one of the craft's hyperdrives. Exiting hyperspace")
                }
            }.runTaskAsynchronously(MSUtils.plugin)
        }
        entry.progressBar.isVisible = false
        processingEntries.remove(craft)
        pendingEntries.remove(craft)
        HyperspaceManager.runnableMap[craft.notificationPlayer?.uniqueId]?.cancel()
        return true
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