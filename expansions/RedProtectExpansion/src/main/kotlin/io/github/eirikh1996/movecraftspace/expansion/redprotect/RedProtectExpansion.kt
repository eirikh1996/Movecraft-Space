package io.github.eirikh1996.movecraftspace.expansion.redprotect

import br.net.fabiozumbi12.RedProtect.Bukkit.API.events.CreateRegionEvent
import br.net.fabiozumbi12.RedProtect.Bukkit.API.events.RedefineRegionEvent
import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect
import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionState
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class RedProtectExpansion : Expansion(), Listener {
    lateinit var redProtectPlugin : RedProtect

    override fun enable() {
        val rp = Bukkit.getPluginManager().getPlugin("RedProtect")
        if (rp !is RedProtect || !rp.isEnabled) {
            logMessage(LogMessageType.CRITICAL, "RedProtect is required, but was not found or is disabled")
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

    @EventHandler(priority = EventPriority.MONITOR)
    fun onCreate(event : CreateRegionEvent) {
        val region = event.region
        if (region == null) return
        for (loc in region.getLimitLocs(region.minY, region.maxY, true)) {
            val planet = PlanetCollection.intersectingOtherPlanetaryOrbit(loc)
            if (planet == null || config.getBoolean("Allow claiming in orbits", false))
                continue
            event.player.sendMessage(COMMAND_PREFIX + MSUtils.ERROR + "Cannot create a region here as it intersect with the planetary orbit of " + planet.name)
            event.isCancelled = true
            break
        }

    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onRedefine(event : RedefineRegionEvent) {
        val region = event.newRegion
        if (region == null) return
        for (loc in region.getLimitLocs(region.minY, region.maxY, true)) {
            val planet = PlanetCollection.intersectingOtherPlanetaryOrbit(loc)
            if (planet == null)
                continue
            event.player.sendMessage(COMMAND_PREFIX + MSUtils.ERROR + "Cannot redefine region here as it intersect with the planetary orbit of " + planet.name)
            event.isCancelled = true
            break
        }
    }
}