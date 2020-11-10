package io.github.eirikh1996.movecraftspace.expansion.lands

import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionState
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import me.angeschossen.lands.api.events.ChunkPreClaimEvent
import me.angeschossen.lands.api.events.LandCreateEvent
import me.angeschossen.lands.api.integration.LandsIntegration
import net.countercraft.movecraft.MovecraftChunk
import org.bukkit.Bukkit
import org.bukkit.Bukkit.getWorld
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class LandsExpansion : Expansion(), Listener {
    lateinit var landsAddon : LandsIntegration
    override fun enable() {
        val l = plugin.server.pluginManager.getPlugin("Lands")
        if (l == null || !l.isEnabled) {
            logMessage(LogMessageType.CRITICAL, "Lands is required, but not found or disabled")
            state = ExpansionState.DISABLED
            return
        }
        landsAddon = LandsIntegration(plugin)
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    override fun allowedArea(p: Player, loc: Location): Boolean {
        val land = landsAddon.getLand(loc)
        return land != null && (land.isTrusted(p.uniqueId) || land.ownerUID == p.uniqueId)
    }

    @EventHandler
    fun onClaim(event : ChunkPreClaimEvent) {
        val planet = PlanetCollection.intersectingOtherPlanetaryOrbit(MovecraftChunk(event.x, event.z, getWorld(event.worldName)))
        if (planet == null)
            return
        event.landPlayer.player!!.sendMessage(MSUtils.COMMAND_PREFIX + "Cannot claim land here as it intersect with the planetary orbit of " + planet.name)
        event.isCancelled = true
    }
}