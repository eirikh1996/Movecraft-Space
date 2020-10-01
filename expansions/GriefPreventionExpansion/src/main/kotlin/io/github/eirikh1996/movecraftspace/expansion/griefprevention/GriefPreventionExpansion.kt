package io.github.eirikh1996.movecraftspace.expansion.griefprevention

import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionState
import me.ryanhamshire.GriefPrevention.GriefPrevention
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

class GriefPreventionExpansion : Expansion(){
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

    }
}