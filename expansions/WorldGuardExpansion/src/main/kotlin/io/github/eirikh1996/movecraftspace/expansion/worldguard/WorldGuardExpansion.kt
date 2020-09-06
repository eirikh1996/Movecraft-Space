package io.github.eirikh1996.movecraftspace.expansion.worldguard

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.bukkit.BukkitWorld
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldguard.LocalPlayer
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.bukkit.WorldGuardPlugin
import com.sk89q.worldguard.protection.ApplicableRegionSet
import com.sk89q.worldguard.protection.managers.RegionManager
import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionState
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import java.lang.reflect.Method

class WorldGuardExpansion : Expansion() {

    lateinit var worldGuardPlugin: WorldGuardPlugin

    private var CAN_BUILD: Method?
    private var GET_REGION_MANAGER: Method?
    private var GET_APPLICABLE_REGIONS : Method?
    init {
        try {
            CAN_BUILD = WorldGuardPlugin::class.java.getDeclaredMethod("canBuild", Player::class.java, Location::class.java)
            GET_REGION_MANAGER = WorldGuardPlugin::class.java.getDeclaredMethod("getRegionManager", World::class.java)
            GET_APPLICABLE_REGIONS = RegionManager::class.java.getDeclaredMethod("getApplicableRegions", Location::class.java)
        } catch (e : Exception) {
            CAN_BUILD = null
            GET_REGION_MANAGER = null
            GET_APPLICABLE_REGIONS = null
        }

    }

    override fun enable() {
        val wg = plugin.server.pluginManager.getPlugin("WorldGuard")
        if (wg !is WorldGuardPlugin || !wg.isEnabled) {
            state = ExpansionState.DISABLED
            return
        }
        worldGuardPlugin = wg
    }

    override fun allowedArea(p: Player, loc: Location): Boolean {
        if (CAN_BUILD != null) {
            return CAN_BUILD!!.invoke(worldGuardPlugin, p, loc) as Boolean
        }
        val regions: ApplicableRegionSet = WorldGuard.getInstance().platform.regionContainer[BukkitWorld(p.getWorld())]!!.getApplicableRegions(BukkitAdapter.asBlockVector(loc))
        val lp: LocalPlayer = worldGuardPlugin.wrapPlayer(p)
        return lp.hasPermission("worldguard.region.bypass." + p.getWorld().getName()) || regions.isMemberOfAll(lp) || regions.isOwnerOfAll(lp)

    }
}