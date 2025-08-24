package io.github.eirikh1996.movecraftspace.expansion.factionsuuid

import com.massivecraft.factions.*
import com.massivecraft.factions.event.LandClaimEvent
import com.massivecraft.factions.perms.PermissibleAction
import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionState
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import net.countercraft.movecraft.MovecraftChunk
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class FactionsUUIDExpansion : Expansion(), Listener{
    var allowEntryInSafezone = false
    var allowEntryInWarzone = false
    override fun allowedArea(p: Player, loc: Location): Boolean {
        val faction = Board.getInstance().getFactionAt(FLocation(loc))
        if (faction.isWilderness || faction.isSafeZone && allowEntryInSafezone || faction.isWarZone && allowEntryInWarzone) return true
        for (s in config.getStringList("Allow entry to factions")) {
            val allowedFaction = Factions.getInstance().getByTag(s)
            if (allowedFaction == null || !faction.equals(allowedFaction))
                continue
            return true
        }
        return faction == Factions.getInstance().wilderness || faction.hasAccess(FPlayers.getInstance().getByPlayer(p), PermissibleAction.BUILD)
    }

    override fun enable() {
        val f = Bukkit.getPluginManager().getPlugin("Factions")
        if (f !is FactionsPlugin || !f.isEnabled) {
            logMessage(LogMessageType.CRITICAL,"FactionsUUID is required, but not found or disabled")
            state = ExpansionState.DISABLED
            return
        }
        saveDefaultConfig()
        allowEntryInSafezone = config.getBoolean("Allow entry to safezone")
        allowEntryInWarzone = config.getBoolean("Allow entry to warzone")
        Bukkit.getServer().pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onClaim(event : LandClaimEvent) {
        val faction = event.faction
        if (faction.isWilderness || faction.isSafeZone || faction.isWarZone || config.getBoolean("Allow claiming in orbits", false)) return
        val fLoc = event.location
        val planet = PlanetCollection.intersectingOtherPlanetaryOrbit(MovecraftChunk(fLoc.x.toInt(), fLoc.z.toInt(), fLoc.world))
        if (planet == null)
            return
        event.getfPlayer().player.sendMessage(COMMAND_PREFIX + ERROR + "Cannot claim land here as it intersect with the planetary orbit of " + planet.name)
        event.isCancelled = true
    }
}