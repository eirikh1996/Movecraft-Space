package io.github.eirikh1996.movecraftspace.expansion.factionsuuid

import com.massivecraft.factions.*
import com.massivecraft.factions.perms.PermissibleAction
import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionState
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

class FactionsUUIDExpansion : Expansion(){
    override fun allowedArea(p: Player, loc: Location): Boolean {
        val faction = Board.getInstance().getFactionAt(FLocation(loc))
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
            logger.severe("FactionsUUID is required, but not found or disabled")
            state = ExpansionState.DISABLED
            return
        }
        saveDefaultConfig()
    }
}