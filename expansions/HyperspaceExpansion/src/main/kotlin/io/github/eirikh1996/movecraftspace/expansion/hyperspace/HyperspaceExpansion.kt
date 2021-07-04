package io.github.eirikh1996.movecraftspace.expansion.hyperspace

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionState
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.command.GravityWellCommand
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.command.HyperdriveCommand
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.command.HyperspaceCommand
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.command.JumpCommand
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.GravityWellManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.HyperdriveManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.HyperspaceManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.sign.HyperspaceSign
import io.github.eirikh1996.movecraftspace.expansion.selection.SelectionSupported
import io.github.eirikh1996.movecraftspace.expansion.selection.Structure
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import net.countercraft.movecraft.craft.CraftManager
import net.countercraft.movecraft.craft.CraftType
import org.bukkit.*
import org.bukkit.Bukkit.getPluginManager
import org.bukkit.inventory.ItemStack
import java.io.File

class HyperspaceExpansion : Expansion(), SelectionSupported {
    lateinit var hyperspaceWorld : World
    lateinit var hyperspaceChargeSound : String
    lateinit var hyperspaceEnterSound : String
    lateinit var hyperspaceExitSound : String
    lateinit var hyperdriveSelectionWand : Material
    lateinit var hypermatter : Material
    lateinit var hypermatterName : String
    lateinit var allowedCraftTypesForHyperspaceSign : Set<String>
    lateinit var allowedCraftTypesForJumpCommand : Set<String>
    var extraMassShadowRangeOfPlanets = 0
    var extraMassShadowRangeOfStars = 0
    val maxHyperdrivesOnCraft = HashMap<String, Int>()
    val maxGravityWellsOnCraft = HashMap<String, Int>()

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
        hyperspaceWorld = hsWorld
        hyperspaceEnterSound = config.getString("Hyperspace enter sound", "entity.enderman.teleport")!!.replace("_",".").toLowerCase()
        hyperspaceChargeSound = config.getString("Hyperspace charge sound", "entity.ender_dragon.ambient")!!.replace("_",".").toLowerCase()
        hyperspaceExitSound = config.getString("Hyperspace exit sound", "entity.enderman.teleport")!!.replace("_",".").toLowerCase()
        hyperdriveSelectionWand = Material.getMaterial(config.getString("Hyperdrive selection wand", "STONE_HOE")!!)!!
        hypermatter = Material.getMaterial(config.getString("Hypermatter.type", "EMERALD")!!) ?: Material.EMERALD
        hypermatterName = config.getString("Hypermatter.name", "")!!
        if (config.contains("Max hyperdrives on craft"))
            config.getConfigurationSection("Max hyperdrives on craft")!!.getValues(false).forEach { t, u -> maxHyperdrivesOnCraft.put(t, u as Int) }
        if (config.contains("Max gravity wells on craft"))
            config.getConfigurationSection("Max gravity wells on craft")!!.getValues(false).forEach { t, u -> maxGravityWellsOnCraft.put(t, u as Int) }
        allowedCraftTypesForHyperspaceSign = config.getStringList("Allowed craft types for hyperspace sign").toSet()
        allowedCraftTypesForJumpCommand = config.getStringList("Allowed craft types for jump command").toSet()
        extraMassShadowRangeOfPlanets = config.getInt("Extra mass shadow range of planets", 0)
        extraMassShadowRangeOfStars = config.getInt("Extra mass shadow range of stars", 0)
        plugin.getCommand("hyperspace")!!.setExecutor(HyperspaceCommand)
        plugin.getCommand("hyperdrive")!!.setExecutor(HyperdriveCommand)
        plugin.getCommand("jump")!!.setExecutor(JumpCommand)
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