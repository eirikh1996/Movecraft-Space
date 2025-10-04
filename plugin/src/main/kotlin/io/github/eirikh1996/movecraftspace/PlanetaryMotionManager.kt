package io.github.eirikh1996.movecraftspace

import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.Planet
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import net.countercraft.movecraft.MovecraftChunk
import net.countercraft.movecraft.MovecraftLocation
import net.countercraft.movecraft.craft.ChunkManager
import net.countercraft.movecraft.craft.Craft
import net.countercraft.movecraft.craft.CraftManager
import net.countercraft.movecraft.craft.PilotedCraft
import net.countercraft.movecraft.craft.type.CraftType
import net.countercraft.movecraft.mapUpdater.MapUpdateManager
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand
import net.countercraft.movecraft.util.MathUtils
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.*
import kotlin.random.Random

object PlanetaryMotionManager : BukkitRunnable() {
    private val updates = LinkedList<PlanetMoveUpdateCommand>()
    private val moonUpdates = HashMap<Planet, MutableSet<UpdateCommand>>()
    private var lastMotionCheck = System.currentTimeMillis()
    private var lastMoonMotionCheck = System.currentTimeMillis()
    private var processedPlanets = false;
    override fun run() {
        processPlanetaryMotion()
        processQueue()
    }

    private fun processPlanetaryMotion() {
        if (System.currentTimeMillis() - lastMotionCheck < Settings.PlanetaryRotationCheckCooldown * 60000 )
            return

        for (pl in PlanetCollection) {
            if (pl.isMoon() || pl.moving)
                continue
            val perimeter = 2 * PI * pl.orbitRadius
            val meterPerDay = perimeter / pl.orbitTime
            val meterPerMinute = meterPerDay / 1440
            val arcLength = meterPerMinute * Settings.PlanetaryRotationCheckCooldown
            val angle = arcLength / pl.orbitRadius
            val newCenter = rotate(angle, pl.orbitCenter, pl.center)
            val translationVector = newCenter.subtract(pl.center)
            updates.add(PlanetMoveUpdateCommand(pl, translationVector))
            if (pl.moons.isNotEmpty()) {
                val moonUpdates = HashSet<UpdateCommand>()
                for (moon in pl.moons) {
                    moonUpdates.add(PlanetMoveUpdateCommand(moon, translationVector, true))
                }
                this.moonUpdates[pl] = moonUpdates
            }

        }
        lastMotionCheck = System.currentTimeMillis()
        processedPlanets = true
    }

    private fun processMoonRotation() {
        for (pl in PlanetCollection) {
            if (!pl.isMoon() || pl.moving)
                continue
            val perimeter = 2 * PI * pl.orbitRadius
            val meterPerDay = perimeter / pl.orbitTime
            val meterPerMinute = meterPerDay / 1440
            val arcLength = meterPerMinute * Settings.PlanetaryRotationCheckCooldown
            val angle = arcLength / pl.orbitRadius
            val newCenter = rotate(angle, pl.orbitCenter, pl.center)
            val translationVector = newCenter.subtract(pl.center)
            updates.add(PlanetMoveUpdateCommand(pl, translationVector))
        }
        processedPlanets = false
        lastMoonMotionCheck = System.currentTimeMillis()
    }

    private fun processQueue() {
        if (MovecraftSpace.instance.averageTicks() < Settings.MinimumTickRate)
            return
        if (updates.isEmpty()) {
            if (processedPlanets)
                processMoonRotation()

            PlanetCollection.saveFile()
            return
        }

        val poll = updates.poll()
        if (moonUpdates.containsKey(poll.planet)) {
            val moonUpdateSet = moonUpdates[poll.planet]!!
            moonUpdateSet.add(poll)
            for (update in moonUpdateSet) {
                if (update !is PlanetMoveUpdateCommand)
                    continue
                translateCraftsToPlanet(update.planet, update.displacement)
            }
            MapUpdateManager.getInstance().scheduleUpdates(moonUpdateSet)
            return
        }
        translateCraftsToPlanet(poll.planet, poll.displacement)
        MapUpdateManager.getInstance().scheduleUpdate(poll)
    }

    private fun translateCraftsToPlanet(planet : Planet, displacement : MovecraftLocation) {
        val newCenter = planet.center.add(displacement)
        val craftsToTeleport = HashSet<PilotedCraft>()
        for (craft in CraftManager.getInstance().getCraftsInWorld(planet.space)) {
            if (craft !is PilotedCraft) {
                continue
            }
            for (ml in craft.hitBox) {
                val dx = abs(newCenter.x - ml.x)
                val dy = abs(newCenter.y - ml.y)
                val dz = abs(newCenter.z - ml.z)
                val distSquared = abs((dx * dx) + (dy * dy) + (dz * dz))
                val radiusSquared = (planet.radius * planet.radius)
                if (distSquared > radiusSquared)
                    continue
                craftsToTeleport.add(craft)
                break
            }
        }
        for (craft in craftsToTeleport) {
            val passthroughBlocks = craft.type.getMaterialSetProperty(CraftType.PASSTHROUGH_BLOCKS)
            val hitBox = craft.hitBox
            val midpoint = hitBox.midPoint
            val y = planet.exitHeight - (hitBox.yLength / 2) - 5
            var destLoc : MovecraftLocation? = null
            while (destLoc == null) {
                val bounds = ExpansionManager.worldBoundrary(planet.destination)
                val minX = (bounds.minPoint.x + (hitBox.xLength / 2)) + 10
                val maxX = (bounds.maxPoint.x - (hitBox.xLength / 2)) - 10
                val minZ = (bounds.minPoint.z + (hitBox.zLength / 2)) + 10
                val maxZ = (bounds.maxPoint.z - (hitBox.zLength / 2)) - 10
                val x = Random.nextInt(minX, maxX)
                val z = Random.nextInt(minZ, maxZ)
                val test = MovecraftLocation(x, y, z)

                if (!MathUtils.withinWorldBorder(planet.destination, test)) {
                    continue
                }
                if (!ExpansionManager.allowedArea(craft.pilot, test.toBukkit(planet.destination))) {
                    continue
                }
                val diff = test.subtract(midpoint)
                val chunks = ChunkManager.getChunks(craft.hitBox, planet.destination, diff.x, diff.y, diff.z)
                MovecraftChunk.addSurroundingChunks(chunks, 3)
                ChunkManager.syncLoadChunks(chunks)
                val testType = test.toBukkit(planet.destination).block.type
                if (!testType.isAir && !passthroughBlocks.contains(testType)) {
                    continue
                }
                val obstructed = Bukkit.getScheduler().callSyncMethod(MovecraftSpace.instance) {
                    var obstructed = false;
                    for (ml in craft.hitBox) {
                        val destHitBoxLoc = ml.add(diff)
                        val destHitBoxLocType = destHitBoxLoc.toBukkit(planet.destination).block.type
                        if (planet.space.equals(planet.destination) && planet.contains(ml.toBukkit(planet.destination))) {
                            obstructed = true
                            break
                        }
                        if (!destHitBoxLocType.isAir && !passthroughBlocks.contains(destHitBoxLocType)) {
                            obstructed = true
                            break
                        }
                        if (!MathUtils.withinWorldBorder(planet.destination, destHitBoxLoc)) {
                            obstructed = true
                            break
                        }

                    }
                    obstructed
                }.get()
                if (obstructed)
                    continue
                destLoc = diff
            }
            craft.pilot!!.sendMessage("Entering " + planet.destination.name)
            craft.translate(planet.destination, destLoc.x, destLoc.y, destLoc.z)
        }
    }

    private fun rotate(angle : Double, origin : MovecraftLocation, offset : MovecraftLocation) : MovecraftLocation {
        val toRotate = offset.subtract(origin)
        val cos = cos(angle)
        val sin = sin(angle)
        val x : Int = round(toRotate.x * cos + toRotate.z * -sin).toInt()
        val y : Int = 0
        val z : Int = round(toRotate.x * sin + toRotate.z * cos).toInt()

        return MovecraftLocation(x, y, z).add(origin)
    }
}
