package io.github.eirikh1996.movecraftspace.expansion.factions

import com.massivecraft.factions.Factions
import com.massivecraft.factions.Perm
import com.massivecraft.factions.entity.*
import com.massivecraft.massivecore.ps.PS
import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionState
import io.github.eirikh1996.movecraftspace.expansion.factions.listener.FactionsListener
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.lang.reflect.Method

class FactionsExpansion : Expansion(){
    var IsMPlayerPermitted : Method? = null
    override fun allowedArea(p: Player, loc: Location): Boolean {
        val faction = BoardColl.get().getFactionAt(PS.valueOf(loc))
        return faction == FactionColl.get().none || if (!factions3) faction.isPermitted(MPerm.getPermBuild(), faction.getRelationTo(MPlayer.get(p))) else IsMPlayerPermitted!!.invoke(faction, MPlayer.get(p), MPerm.getPermBuild()) as Boolean
    }

    override fun enable() {
        val f = Bukkit.getPluginManager().getPlugin("Factions")
        if (f !is Factions || !f.isEnabled) {
            logger.severe("Factions is required, but not found or disabled")
            state = ExpansionState.DISABLED
            return
        }
        factions3 = f.description.version.split(".")[0].toInt() >= 3
        if (factions3) {
            IsMPlayerPermitted = Faction::class.java.getDeclaredMethod("isPlayerPermitted", MPlayer::class.java, MPerm::class.java)
        }
        plugin.server.pluginManager.registerEvents(FactionsListener, plugin)
    }

    companion object {
        var factions3 = false
    }

}