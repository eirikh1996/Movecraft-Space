package io.github.eirikh1996.movecraftspace.expansion.towny

import com.palmergames.bukkit.towny.Towny
import com.palmergames.bukkit.towny.TownyAPI
import com.palmergames.bukkit.towny.`object`.TownyPermission
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil
import com.palmergames.bukkit.towny.utils.ResidentUtil
import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionState
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player

class TownyExpansion : Expansion() {
    lateinit var towny : Towny

    override fun enable() {
        val t = plugin.server.pluginManager.getPlugin("Towny")
        if (t !is Towny || !t.isEnabled) {
            state = ExpansionState.DISABLED
            return
        }
        towny = t

    }

    override fun allowedArea(p: Player, loc: Location): Boolean {
        return PlayerCacheUtil.getCachePermission(p, loc, Material.STONE, TownyPermission.ActionType.BUILD)
    }
}