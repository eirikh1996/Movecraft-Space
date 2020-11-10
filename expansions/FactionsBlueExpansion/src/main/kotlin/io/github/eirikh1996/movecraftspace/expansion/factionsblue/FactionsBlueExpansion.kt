package io.github.eirikh1996.movecraftspace.expansion.factionsblue

import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionState
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import me.zysea.factions.FPlugin
import me.zysea.factions.events.FPlayerClaimEvent
import me.zysea.factions.objects.Claim
import net.countercraft.movecraft.MovecraftChunk
import org.bukkit.Bukkit.getPluginManager
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

class FactionsBlueExpansion : Expansion() {
    lateinit var factionsBluePlugin : FPlugin
    var allowEntryInSafezone = false
    var allowEntryInWarzone = false
    override fun enable() {
        val fb = getPluginManager().getPlugin("FactionsBlue")
        if (fb !is FPlugin || !fb.isEnabled) {
            logMessage(LogMessageType.CRITICAL, "FactionsBlue is required, but was not found or is disabled")
            state = ExpansionState.DISABLED
            return
        }
        saveDefaultConfig()
        factionsBluePlugin = fb
        allowEntryInSafezone = config.getBoolean("Allow entry to safezone")
        allowEntryInWarzone = config.getBoolean("Allow entry to warzone")
    }

    override fun allowedArea(p: Player, loc: Location): Boolean {
        val claim = Claim(loc)
        val faction = factionsBluePlugin.claims.getOwner(claim)
        if (faction.isWilderness || faction.isSafezone && allowEntryInSafezone || faction.isWarzone && allowEntryInWarzone)
            return true
        return !faction.isWilderness
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onClaim(event : FPlayerClaimEvent) {
        val claim = event.claim
        val planet = PlanetCollection.intersectingOtherPlanetaryOrbit(MovecraftChunk(claim.x, claim.z, claim.world))
        if (planet == null) return
        event.player.sendMessage(COMMAND_PREFIX + ERROR + "Cannot claim faction land here as it intersects with planetary orbit of " + planet.name)
        event.isCancelled = true
    }
}