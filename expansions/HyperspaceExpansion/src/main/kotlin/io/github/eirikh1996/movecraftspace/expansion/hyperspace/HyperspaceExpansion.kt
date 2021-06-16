package io.github.eirikh1996.movecraftspace.expansion.hyperspace

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
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import net.countercraft.movecraft.craft.CraftManager
import net.countercraft.movecraft.craft.CraftType
import org.bukkit.*
import org.bukkit.Bukkit.getPluginManager
import org.bukkit.inventory.ItemStack

class HyperspaceExpansion : Expansion(), SelectionSupported {
    lateinit var hyperspaceWorld : World
    lateinit var hyperspaceChargeSound : Sound
    lateinit var hyperspaceEnterSound : Sound
    lateinit var hyperspaceExitSound : Sound
    lateinit var hyperdriveSelectionWand : Material
    lateinit var hypermatter : Material
    lateinit var hypermatterName : String
    val allowedCraftTypesForHyperspaceSign = HashSet<CraftType>()
    val allowedCraftTypesForJumpCommand = HashSet<CraftType>()
    val maxHyperdrivesOnCraft = HashMap<String, Int>()

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
        hyperspaceEnterSound = Sound.valueOf(config.getString("Hyperspace enter sound", "ENTITY_ENDERMAN_TELEPORT")!!)
        hyperspaceChargeSound = Sound.valueOf(config.getString("Hyperspace charge sound", "BLOCK_STONE_BREAK")!!)
        hyperspaceExitSound = Sound.valueOf(config.getString("Hyperspace exit sound", "ENTITY_ENDERMAN_TELEPORT")!!)
        hyperdriveSelectionWand = Material.getMaterial(config.getString("Hyperdrive selection wand", "STONE_HOE")!!)!!
        hypermatter = Material.getMaterial(config.getString("Hypermatter.type", "EMERALD")!!) ?: Material.EMERALD
        hypermatterName = config.getString("Hypermatter.name", "")!!
        if (config.contains("Max hyperdrives on craft"))
            config.getConfigurationSection("Max hyperdrives on craft")!!.getValues(false).forEach { t, u -> maxHyperdrivesOnCraft.put(t, u as Int) }
        config.getStringList("Allowed craft types for hyperspace sign").forEach { s -> allowedCraftTypesForHyperspaceSign.add(CraftManager.getInstance().getCraftTypeFromString(s)) }
        config.getStringList("Allowed craft types for jump command").forEach { s -> allowedCraftTypesForJumpCommand.add(CraftManager.getInstance().getCraftTypeFromString(s)) }
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