package io.github.eirikh1996.movecraftspace.expansion.hyperspace

import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionState
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.command.HyperspaceCommand
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import org.bukkit.Bukkit
import org.bukkit.Bukkit.getConsoleSender
import org.bukkit.Bukkit.getPluginManager
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player

class HyperspaceExpansion : Expansion() {
    lateinit var hyperspaceWorld : World

    override fun enable() {
        saveDefaultConfig()
        val hsWorld = Bukkit.getWorld(config.getString("Hyperspace world")!!)
        if (hsWorld == null) {
            logMessage(LogMessageType.CRITICAL, "World designated as hyperspace world does not exist on the server")
            state = ExpansionState.DISABLED
            return
        } else if (PlanetCollection.worldIsPlanet(hsWorld)) {
            logMessage(LogMessageType.CRITICAL, "World designated as hyperspace world is already a planet world. Hyperspace worlds can neither be planet nor space worlds")
            state = ExpansionState.DISABLED
            return
        } else if (isSpaceWorld(hsWorld)) {
            logMessage(LogMessageType.CRITICAL, "World designated as hyperspace world is already a space world. Hyperspace worlds can neither be planet nor space worlds")
            state = ExpansionState.DISABLED
            return
        }
        HyperspaceManager.loadFile()
        hyperspaceWorld = hsWorld
        plugin.getCommand("hyperspace")!!.setExecutor(HyperspaceCommand)
        HyperspaceManager.runTaskTimerAsynchronously(plugin, 0, 1)
        getPluginManager().registerEvents(HyperspaceManager, plugin)
    }

    override fun load() {
        instance = this
    }

    private fun isSpaceWorld(world: World) : Boolean {
        for (planet in PlanetCollection) {
            if (!world.equals(planet.space))
                continue
            return true
        }
        return false
    }

    companion object {
        lateinit var instance : HyperspaceExpansion
    }
}