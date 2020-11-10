package io.github.eirikh1996.movecraftspace.expansion.towny

import com.palmergames.bukkit.towny.Towny
import com.palmergames.bukkit.towny.TownyAPI
import com.palmergames.bukkit.towny.`object`.TownyPermission
import com.palmergames.bukkit.towny.event.TownClaimEvent
import com.palmergames.bukkit.towny.event.TownPreClaimEvent
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil
import com.palmergames.bukkit.towny.utils.ResidentUtil
import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionState
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import net.countercraft.movecraft.MovecraftChunk
import org.bukkit.Bukkit.getPluginManager
import org.bukkit.Bukkit.getWorld
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class TownyExpansion : Expansion(), Listener {
    lateinit var towny : Towny

    override fun enable() {
        val t = plugin.server.pluginManager.getPlugin("Towny")
        if (t !is Towny || !t.isEnabled) {
            logMessage(LogMessageType.CRITICAL, "Towny is required, but was not found or is disabled")
            state = ExpansionState.DISABLED
            return
        }
        towny = t
        getPluginManager().registerEvents(this, plugin)
    }

    override fun allowedArea(p: Player, loc: Location): Boolean {
        return PlayerCacheUtil.getCachePermission(p, loc, Material.STONE, TownyPermission.ActionType.BUILD)
    }

    @EventHandler
    fun onClaim(event : TownPreClaimEvent) {
        val tb = event.townBlock
        val planet = PlanetCollection.intersectingOtherPlanetaryOrbit(MovecraftChunk(tb.x, tb.z, getWorld(tb.world.name)))
        if (planet == null)
            return
        event.player.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Cannot claim town blocks here as it intersect with the planetary orbit of " + planet.name)
        event.isCancelled = true
    }
}