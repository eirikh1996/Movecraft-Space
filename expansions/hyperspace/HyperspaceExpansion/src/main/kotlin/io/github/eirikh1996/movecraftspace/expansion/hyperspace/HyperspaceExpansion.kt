package io.github.eirikh1996.movecraftspace.expansion.hyperspace

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionState
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.ExpansionSettings.allowedCraftTypesForHyperspaceSign
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.ExpansionSettings.allowedCraftTypesForJumpCommand
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.ExpansionSettings.extraMassShadowRangeOfPlanets
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.ExpansionSettings.extraMassShadowRangeOfStars
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.ExpansionSettings.maxGravityWellsOnCraft
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.ExpansionSettings.maxHyperdrivesOnCraft
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.command.*
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.GravityWellManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.HyperdriveManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.HyperspaceManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.sign.HyperspaceSign
import io.github.eirikh1996.movecraftspace.expansion.selection.SelectionSupported
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import org.bukkit.*
import org.bukkit.Bukkit.getPluginManager
import java.io.File
import kotlin.math.max

class HyperspaceExpansion : Expansion(), SelectionSupported {


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
        val packgeName = plugin.server.javaClass.`package`.name
        val is1_11 = packgeName.substring(packgeName.lastIndexOf(".") + 1).split("_")[1].toInt() <= 11
        var sourcePathPrefix = (if (is1_11) "1_11/" else if (Settings.IsLegacy) "1_12/" else "")
        saveResource(sourcePathPrefix + "beaconstructure.yml", false, "beaconstructure.yml")
        sourcePathPrefix = (if (Settings.IsLegacy) "1_12/" else "")
        if (!File(dataFolder, "hyperdrives").exists()) {
            for (i in 1..2) {
                saveResource(sourcePathPrefix + "hyperdrives/ExampleDrive" + i + ".yml", false, "hyperdrives/ExampleDrive" + i + ".yml")
            }

        }
        if (!File(dataFolder, "gravitywells").exists()) {
            for (i in 1..2) {
                saveResource(sourcePathPrefix + "gravitywells/ExampleWell" + i + ".yml", false, "gravitywells/ExampleWell" + i + ".yml")
            }

        }
        HyperspaceManager.loadFile()
        ExpansionSettings.hyperspaceWorld = hsWorld
        ExpansionSettings.hyperspaceEnterSound = config.getString("Hyperspace enter sound", "entity.enderman.teleport")!!.toLowerCase()
        ExpansionSettings.hyperspaceChargeSound = config.getString("Hyperspace charge sound", "entity.ender_dragon.ambient")!!.toLowerCase()
        ExpansionSettings.hyperspaceExitSound = config.getString("Hyperspace exit sound", "entity.enderman.teleport")!!.toLowerCase()
        ExpansionSettings.hypermatter = Material.getMaterial(config.getString("Hypermatter.type", "EMERALD")!!) ?: Material.EMERALD
        ExpansionSettings.hypermatterName = config.getString("Hypermatter.name", "")!!
        if (config.contains("Max hyperdrives on craft"))
            config.getConfigurationSection("Max hyperdrives on craft")!!.getValues(false).forEach { (t, u) -> maxHyperdrivesOnCraft.put(t, u as Int) }
        if (config.contains("Max gravity wells on craft"))
            config.getConfigurationSection("Max gravity wells on craft")!!.getValues(false).forEach { (t, u) -> maxGravityWellsOnCraft.put(t, u as Int) }
        allowedCraftTypesForHyperspaceSign = config.getStringList("Allowed craft types for hyperspace sign").toSet()
        allowedCraftTypesForJumpCommand = config.getStringList("Allowed craft types for jump command").toSet()
        extraMassShadowRangeOfPlanets = max(config.getInt("Extra mass shadow range.planets", 0), 0)
        extraMassShadowRangeOfStars = max(config.getInt("Extra mass shadow range.stars", 0), 0)
        plugin.getCommand("hyperspace")!!.setExecutor(HyperspaceCommand)
        plugin.getCommand("hyperdrive")!!.setExecutor(HyperdriveCommand)
        plugin.getCommand("jump")!!.setExecutor(if (Settings.IsMovecraft8) Movecraft8JumpCommand else Movecraft7JumpCommand)
        plugin.getCommand("gravitywell")!!.setExecutor(GravityWellCommand)
        HyperspaceManager.runTaskTimerAsynchronously(plugin, 0, 1)
        getPluginManager().registerEvents(HyperspaceManager, plugin)
        HyperdriveManager.loadHyperdrives()
        getPluginManager().registerEvents(HyperdriveManager, plugin)
        GravityWellManager.loadGravityWells()
        getPluginManager().registerEvents(GravityWellManager, plugin)
        getPluginManager().registerEvents(HyperspaceSign, plugin)
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