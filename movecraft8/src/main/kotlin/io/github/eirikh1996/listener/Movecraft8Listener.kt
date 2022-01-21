package io.github.eirikh1996.listener

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.objects.Planet
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.objects.StarCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils.hitboxObstructed
import net.countercraft.movecraft.Movecraft
import net.countercraft.movecraft.MovecraftChunk
import net.countercraft.movecraft.MovecraftLocation
import net.countercraft.movecraft.craft.ChunkManager
import net.countercraft.movecraft.craft.Craft
import net.countercraft.movecraft.craft.CraftManager
import net.countercraft.movecraft.craft.PlayerCraft
import net.countercraft.movecraft.craft.type.CraftType
import net.countercraft.movecraft.events.CraftPreTranslateEvent
import net.countercraft.movecraft.events.CraftReleaseEvent
import net.countercraft.movecraft.events.CraftSinkEvent
import net.countercraft.movecraft.libs.net.kyori.adventure.text.Component
import net.countercraft.movecraft.libs.net.kyori.adventure.text.TextComponent
import net.countercraft.movecraft.mapUpdater.MapUpdateManager
import net.countercraft.movecraft.mapUpdater.update.ExplosionUpdateCommand
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand
import net.countercraft.movecraft.util.hitboxes.BitmapHitBox
import net.countercraft.movecraft.util.MathUtils
import org.bukkit.Bukkit
import org.bukkit.Bukkit.getScheduler
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import java.lang.Integer.max
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.Future
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.min
import kotlin.random.Random

object Movecraft8Listener : Listener {
    var SET_CANCELLED : Method?
    val plugin : Plugin
    init {
        try {
            SET_CANCELLED = ChunkUnloadEvent::class.java.getDeclaredMethod("setCancelled")
        } catch (e : Exception) {
            SET_CANCELLED = null
        }
        plugin = Bukkit.getPluginManager().getPlugin("Movecraft-Space")!!
    }

    @EventHandler
    fun onCraftPreTranslate (event : CraftPreTranslateEvent) {
        val craft = event.craft
        if (!craft.type.getBoolProperty(CraftType.CAN_SWITCH_WORLD) || craft !is PlayerCraft) {
            return
        }
        val hitBox = craft.hitBox
        var planet : Planet? = PlanetCollection.getCorrespondingPlanet(craft.world)
        if (planet == null) {
            for (ml in hitBox) {
                val destination = ml.translate(event.dx, event.dy, event.dz)
                if (craft.hitBox.contains(destination)) {
                    continue
                }
                planet = PlanetCollection.getPlanetAt(destination.toBukkit(craft.w))
                if (planet == null) {
                    continue
                }
                break
            }
        }
        if (planet == null) {
            return
        }
        val destWorld = if (planet.destination == craft.w) {
            planet.space;
        } else {
            planet.destination
        }
        if (craft.w == planet.destination && hitBox.maxY + event.dy < planet.exitHeight) {
            return
        }
        val audience = craft.audience ?: return
        if (destWorld == planet.destination) {
            audience.sendMessage(Component.text().content("Entering " + destWorld.name).asComponent())
        } else {
            audience.sendMessage(Component.text().content("Exiting " + craft.world.name).asComponent())
        }
        if (craft.cruising) {
            craft.cruising = false
        }
        val midpoint = craft.hitBox.midPoint
        val displacement = if (destWorld == planet.destination) {
            //Space craft is in space
            val y = planet.exitHeight - (hitBox.yLength / 2) - 5
            var destLoc : MovecraftLocation? = null
            while (destLoc == null) {
                val bounds = ExpansionManager.worldBoundrary(destWorld)
                val minX = (bounds.minPoint.x + (hitBox.xLength / 2)) + 10
                val maxX = (bounds.maxPoint.x - (hitBox.xLength / 2)) - 10
                val minZ = (bounds.minPoint.z + (hitBox.zLength / 2)) + 10
                val maxZ = (bounds.maxPoint.z - (hitBox.zLength / 2)) - 10
                val x = Random.nextInt(minX, maxX)
                val z = Random.nextInt(minZ, maxZ)
                val test = MovecraftLocation(x, y, z)

                if (!MathUtils.withinWorldBorder(destWorld, test)) {
                    continue
                }

                if (!ExpansionManager.allowedArea(craft.pilot, test.toBukkit(destWorld))) {
                    continue
                }
                val diff = test.subtract(midpoint)
                val chunks = ChunkManager.getChunks(craft.hitBox, destWorld, diff.x, diff.y, diff.z)
                MovecraftChunk.addSurroundingChunks(chunks, 3)
                ChunkManager.syncLoadChunks(chunks)
                val testType = test.toBukkit(destWorld).block.type
                if (!testType.name.endsWith("AIR") && !craft.type.getMaterialSetProperty(CraftType.PASSTHROUGH_BLOCKS).contains(testType)) {
                    continue
                }
                val obstructed = hitboxObstructed(craft.hitBox.asSet(), craft.type.getMaterialSetProperty(CraftType.PASSTHROUGH_BLOCKS), planet, destWorld, diff)
                if (obstructed)
                    continue
                destLoc = diff
            }
            destLoc
        } else {
            //Space craft is on a planet
            var destLoc : MovecraftLocation? = null
            while (destLoc == null) {
                val maxY = min(planet.center.y + planet.radius + 160, destWorld.maxHeight - 5)
                val minY = max(planet.center.y - planet.radius - 160, if (Settings.IsV1_17) destWorld.minHeight + 5 else 10)
                val x = Random.nextInt(planet.center.x - planet.radius - 160, planet.center.x + planet.radius + 160)
                val y = Random.nextInt(minY, maxY)
                val z = Random.nextInt(planet.center.z - planet.radius - 160, planet.center.z + planet.radius + 160)
                val test = MovecraftLocation(x, max(min(y, 252 - (craft.hitBox.yLength / 2)), (craft.hitBox.yLength / 2) + 3), z)
                if (planet.contains(test.toBukkit(destWorld))) {
                    continue
                }

                if (!ExpansionManager.allowedArea(craft.pilot, test.toBukkit(destWorld))) {
                    continue
                }
                if (StarCollection.getStarAt(test.toBukkit(destWorld)) != null)
                    continue
                val diff = test.subtract(midpoint)
                val chunks = ChunkManager.getChunks(craft.hitBox, destWorld, diff.x, diff.y, diff.z)
                MovecraftChunk.addSurroundingChunks(chunks, 3)
                ChunkManager.syncLoadChunks(chunks)
                val obstructed = hitboxObstructed(craft.hitBox.asSet(), craft.type.getMaterialSetProperty(CraftType.PASSTHROUGH_BLOCKS), planet, destWorld, diff)
                if (obstructed)
                    continue
                destLoc = diff
            }
            destLoc
        }
        event.world = destWorld
        event.dx = displacement.x
        event.dy = displacement.y
        event.dz = displacement.z

    }

    @EventHandler(
        priority = EventPriority.MONITOR
    )
    fun onSink(event : CraftSinkEvent) {
        if (!Settings.ExplodeSinkingCraftsInWorlds.contains(event.craft.w.name)) {
            return
        }
        event.isCancelled = true
        val explosionLocations = HashSet<UpdateCommand>()
        val hitBox = event.craft.hitBox
        for (x in hitBox.minX..hitBox.maxX step 5) {
            for (y in hitBox.minY..hitBox.maxY step 5) {
                for (z in hitBox.minZ..hitBox.maxZ step 5) {
                    if (!hitBox.contains(x, y, z))
                        continue
                    explosionLocations.add(ExplosionUpdateCommand(Location(event.craft.w, x.toDouble(), y.toDouble(), z.toDouble()), 6f))
                }
            }
        }
        if (explosionLocations.isEmpty()) {
            explosionLocations.add(ExplosionUpdateCommand(hitBox.midPoint.toBukkit(event.craft.w), 6f))
        }
        val collapsed = BitmapHitBox(hitBox)
        event.craft.hitBox = BitmapHitBox()
        CraftManager.getInstance().removeCraft(event.craft, CraftReleaseEvent.Reason.SUNK)
        MapUpdateManager.getInstance().scheduleUpdates(explosionLocations)
        object : BukkitRunnable() {
            override fun run() {
                event.craft.collapsedHitBox.addAll(collapsed.filter {
                        loc -> !loc.toBukkit(event.craft.w).block.type.name.endsWith("AIR")
                })
                Movecraft.getInstance().asyncManager.addWreck(event.craft)
            }

        }.runTaskLater(plugin, 3)
    }

    @EventHandler
    fun onRelease(event : CraftReleaseEvent) {
        if (event.reason == CraftReleaseEvent.Reason.FORCE ||
            event.reason == CraftReleaseEvent.Reason.DISCONNECT ||
            event.reason == CraftReleaseEvent.Reason.SUNK ||
            !Settings.DisableReleaseInPlanetaryOrbits) {
            return
        }
        val craft = event.craft
        for (ml in craft.hitBox) {
            val intersecting = PlanetCollection.intersectingOtherPlanetaryOrbit(ml.toBukkit(craft.w)) ?: continue
            craft.notificationPlayer!!.sendMessage("You cannot release your craft here as the craft intersects with the planetary orbit of " + intersecting.name)
            event.isCancelled = true
            break
        }
    }


}