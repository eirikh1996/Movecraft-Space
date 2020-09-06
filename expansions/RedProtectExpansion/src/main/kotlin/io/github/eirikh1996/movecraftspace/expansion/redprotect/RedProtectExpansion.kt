package io.github.eirikh1996.movecraftspace.expansion.redprotect

import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect
import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionState
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

class RedProtectExpansion : Expansion() {
    lateinit var redProtectPlugin : RedProtect

    override fun enable() {
        val rp = Bukkit.getPluginManager().getPlugin("RedProtect")
        if (rp !is RedProtect || !rp.isEnabled) {
            state = ExpansionState.DISABLED
            return
        }
        redProtectPlugin = rp;
    }
    override fun allowedArea(p: Player, loc: Location): Boolean {
        val api = redProtectPlugin.api
        val region = api.getRegion(loc)
        return region == null || region.canBuild(p)
    }
}