package io.github.eirikh1996.movecraftspace.expansion.griefprevention

import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionState
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import me.ryanhamshire.GriefPrevention.Claim
import me.ryanhamshire.GriefPrevention.GriefPrevention
import me.ryanhamshire.GriefPrevention.events.ClaimCreatedEvent
import me.ryanhamshire.GriefPrevention.events.ClaimExtendEvent
import org.bukkit.Bukkit
import org.bukkit.Bukkit.getPlayer
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class GriefPreventionExpansion : Expansion(), Listener{
    var griefPrevention : GriefPrevention? = null
    override fun allowedArea(p: Player, loc: Location): Boolean {
        val foundClaim = griefPrevention!!.dataStore.getClaimAt(loc, false, null)
        if (foundClaim == null)
            return true
        return foundClaim.allowAccess(p) == null
    }

    override fun enable() {
        val gp = Bukkit.getPluginManager().getPlugin("GriefPrevention")
        if (gp !is GriefPrevention || !gp.isEnabled) {
            logger.severe("GriefPrevention is required, but not found or disabled")
            state = ExpansionState.DISABLED
            return
        }
        griefPrevention = gp
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onClaimCreate(event : ClaimCreatedEvent) {
        val claim = event.claim
        if (claim.isAdminClaim)
            return
        val lesser = claim.lesserBoundaryCorner
        val test = lesser.clone()
        for (x in 0..claim.width) {
            test.z = lesser.z
            for (z in 0..claim.height) {
                test.add(x.toDouble(), 127.01, z.toDouble())
                val planet = PlanetCollection.intersectingOtherPlanetaryOrbit(test)
                if (planet == null)
                    continue
                event.creator.sendMessage(COMMAND_PREFIX + ERROR + "Cannot create claim here as claim intersect with the orbit of " + planet.name)
                event.isCancelled = true
                return
            }
        }
    }

    @EventHandler
    fun onExtend(event : ClaimExtendEvent) {
        val claim = event.claim
        if (claim.isAdminClaim)
            return
        val lesser = claim.lesserBoundaryCorner
        val test = lesser.clone()
        for (x in 0..claim.width) {
            test.z = lesser.z
            for (z in 0..claim.height) {
                test.add(x.toDouble(), 127.01, z.toDouble())
                val planet = PlanetCollection.intersectingOtherPlanetaryOrbit(test)
                if (planet == null)
                    continue
                getPlayer(claim.ownerID)!!.sendMessage(COMMAND_PREFIX + ERROR + "Cannot create claim here as claim intersect with the orbit of " + planet.name)
                event.isCancelled = true
                return
            }
        }
    }
}