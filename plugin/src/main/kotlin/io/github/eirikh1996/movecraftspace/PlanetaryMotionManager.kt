package io.github.eirikh1996.movecraftspace

import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.Planet
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import net.countercraft.movecraft.mapUpdater.MapUpdateManager
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand
import org.bukkit.World
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

object PlanetaryMotionManager : BukkitRunnable() {
    val updates = LinkedList<PlanetMoveUpdateCommand>()
    val moonUpdates = HashMap<Planet, MutableSet<UpdateCommand>>()
    var lastMotionCheck = System.currentTimeMillis()
    var lastMoonMotionCheck = System.currentTimeMillis()
    var processedPlanets = false;
    override fun run() {
        processPlanetaryMotion()
        processQueue()
    }

    fun processPlanetaryMotion() {
        if (System.currentTimeMillis() - lastMotionCheck < 300000)
            return

        for (pl in PlanetCollection) {
            if (pl.isMoon())
                continue
            val angle = (2 * PI) / pl.orbitTime
            val newCenter = pl.center.rotate(angle, pl.orbitCenter)
            val translationVector = newCenter.subtract(pl.center)
            updates.add(PlanetMoveUpdateCommand(pl, translationVector))
            if (!pl.moons.isEmpty()) {
                val moonUpdates = HashSet<UpdateCommand>()
                for (moon in pl.moons) {
                    moonUpdates.add(PlanetMoveUpdateCommand(moon, translationVector, true))
                }
                this.moonUpdates.put(pl, moonUpdates)
            }
        }
        lastMotionCheck = System.currentTimeMillis()
        processedPlanets = true
    }

    private fun processMoonRotation() {
        for (pl in PlanetCollection) {
            if (!pl.isMoon())
                continue
            val angle = (2 * PI) / pl.orbitTime
            val newCenter = pl.center.rotate(angle, pl.orbitCenter)
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
            MapUpdateManager.getInstance().scheduleUpdates(moonUpdateSet)
            return
        }
        MapUpdateManager.getInstance().scheduleUpdate(poll)
    }

}
