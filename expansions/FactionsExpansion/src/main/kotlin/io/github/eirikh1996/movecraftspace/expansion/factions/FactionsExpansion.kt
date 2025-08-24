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
import kotlin.properties.Delegates

class FactionsExpansion : Expansion(){
    var IsMPlayerPermitted : Method? = null
    override fun allowedArea(p: Player, loc: Location): Boolean {
        val faction = BoardColl.get().getFactionAt(PS.valueOf(loc))
        for (s in config.getStringList("Allow entry to factions")) {
            val allowedFaction = FactionColl.get().getByName(s)
            if (allowedFaction == null || !faction.equals(allowedFaction))
                continue
            return true
        }
        return faction == FactionColl.get().none || faction.isPermitted(MPerm.getPermBuild().id, faction.getRelationTo(MPlayer.get(p)).id)
    }

    override fun enable() {
        val f = Bukkit.getPluginManager().getPlugin("Factions")
        if (f !is Factions || !f.isEnabled) {
            logMessage(LogMessageType.CRITICAL,"Factions is required, but not found or disabled")
            state = ExpansionState.DISABLED
            return
        }
        saveDefaultConfig()
        factions3 = f.description.version.split(".")[0].toInt() >= 3
        if (factions3) {
            IsMPlayerPermitted = Faction::class.java.getDeclaredMethod("isPlayerPermitted", MPlayer::class.java, MPerm::class.java)
        }
        plugin.server.pluginManager.registerEvents(FactionsListener, plugin)
        AllowClaimingInOrbits =  config.getBoolean("Allow claiming in orbits", false)
    }

    companion object {
        var factions3 = false
        var AllowClaimingInOrbits = false
    }

}