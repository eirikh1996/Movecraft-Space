package io.github.eirikh1996.movecraftspace.expansion.factions

import com.massivecraft.factions.Factions
import com.massivecraft.factions.Perm
import com.massivecraft.factions.entity.BoardColl
import com.massivecraft.factions.entity.FactionColl
import com.massivecraft.factions.entity.MPerm
import com.massivecraft.factions.entity.MPlayer
import com.massivecraft.massivecore.ps.PS
import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionState
import io.github.eirikh1996.movecraftspace.expansion.factions.listener.FactionsListener
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

class FactionsExpansion : Expansion(){
    override fun allowedArea(p: Player, loc: Location): Boolean {
        val faction = BoardColl.get().getFactionAt(PS.valueOf(loc))
        return faction == FactionColl.get().none || faction.isPermitted(MPerm.getPermBuild(), faction.getRelationTo(MPlayer.get(p)))
    }

    override fun enable() {
        val f = Bukkit.getPluginManager().getPlugin("Factions")
        if (f !is Factions || !f.isEnabled) {
            logger.severe("Factions is required, but not found or disabled")
            state = ExpansionState.DISABLED
            return
        }
        plugin.server.pluginManager.registerEvents(FactionsListener, plugin)
    }

}