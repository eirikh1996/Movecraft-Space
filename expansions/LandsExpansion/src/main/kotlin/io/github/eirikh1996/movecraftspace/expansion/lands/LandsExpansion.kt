package io.github.eirikh1996.movecraftspace.expansion.lands

import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionState
import me.angeschossen.lands.api.integration.LandsIntegration
import org.bukkit.Location
import org.bukkit.entity.Player

class LandsExpansion : Expansion() {
    lateinit var landsAddon : LandsIntegration
    override fun enable() {
        val l = plugin.server.pluginManager.getPlugin("Lands")
        if (l == null || !l.isEnabled) {
            logMessage(LogMessageType.CRITICAL, "Lands is required, but not found or disabled")
            state = ExpansionState.DISABLED
            return
        }
        landsAddon = LandsIntegration(plugin)
    }

    override fun allowedArea(p: Player, loc: Location): Boolean {
        val land = landsAddon.getLand(loc)
        return land != null && (land.isTrusted(p.uniqueId) || land.ownerUID == p.uniqueId)
    }
}