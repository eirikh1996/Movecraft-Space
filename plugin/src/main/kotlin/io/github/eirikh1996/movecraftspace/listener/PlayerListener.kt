package io.github.eirikh1996.movecraftspace.listener

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import net.countercraft.movecraft.craft.Craft
import net.countercraft.movecraft.craft.CraftManager
import net.countercraft.movecraft.utils.MathUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import java.lang.Math.random
import java.util.*
import kotlin.collections.HashSet
import kotlin.math.min
import kotlin.random.Random

object PlayerListener : Listener {
    val teleportingPlayers = HashSet<UUID>()
    val disabledPlayers = HashSet<UUID>()

    @EventHandler
    fun onPlayerMove(event : PlayerMoveEvent) {
        val planet = PlanetCollection.getPlanetAt(event.to!!)
        if (planet == null || planet.destination == event.to!!.world && event.to!!.blockY < planet.exitHeight || teleportingPlayers.contains(event.player.uniqueId) || disabledPlayers.contains(event.player.uniqueId) || !Settings.AllowPlayersTeleportationToPlanets) {
            return
        }
        val craftPlayerIsOn = CraftManager.getInstance().getCraftsInWorld(event.to?.world!!).firstOrNull { craft -> MathUtils.locIsNearCraftFast(craft, MathUtils.bukkit2MovecraftLoc(event.to!!)) }
        if (craftPlayerIsOn != null)
            return
        var dest : Location? = null
        while (dest == null) {
            dest = if (planet.destination == event.to!!.world) {
                val coordArray = MSUtils.randomCoords(planet.minX - 50, planet.maxX + 50, 0, 255, planet.minZ - 50, planet.maxz + 50)
                val test = Location(planet.space, coordArray[0].toDouble(), coordArray[1].toDouble(), coordArray[2].toDouble())
                if (planet.contains(test)) {
                    continue
                }
                test
            } else if (planet.contains(event.to!!)) {
                val coordArray = ExpansionManager.worldBoundrary(planet.destination)
                var test : Location? = null
                while (test == null || !ExpansionManager.allowedArea(event.player, test)) {
                    test = Location(planet.destination, Random.nextDouble(coordArray.minPoint.x.toDouble(), coordArray.maxPoint.x.toDouble()), (planet.exitHeight - 5).toDouble(), Random.nextDouble(coordArray.minPoint.z.toDouble(), coordArray.maxPoint.z.toDouble()))
                }
                test


            } else {
                return
            }
        }
        event.player.teleport(dest)
        teleportingPlayers.remove(event.player.uniqueId)

    }


}