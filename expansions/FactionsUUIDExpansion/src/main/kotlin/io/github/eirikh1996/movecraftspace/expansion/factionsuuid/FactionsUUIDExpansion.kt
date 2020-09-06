package io.github.eirikh1996.movecraftspace.expansion.factionsuuid

import com.massivecraft.factions.Board
import com.massivecraft.factions.FLocation
import com.massivecraft.factions.FPlayers
import com.massivecraft.factions.Factions
import com.massivecraft.factions.perms.PermissibleAction
import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionState
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

class FactionsUUIDExpansion : Expansion(){
    override fun allowedArea(p: Player, loc: Location): Boolean {
        val faction = Board.getInstance().getFactionAt(FLocation(loc))
        return faction == Factions.getInstance().wilderness || faction.hasAccess(FPlayers.getInstance().getByPlayer(p), PermissibleAction.BUILD)
    }

    override fun enable() {
        val f = Bukkit.getPluginManager().getPlugin("Factions")
        if (f !is Factions || !f.isEnabled) {
            logger.severe("Factions is required, but not found or disabled")
            state = ExpansionState.DISABLED
            return
        }
    }
}