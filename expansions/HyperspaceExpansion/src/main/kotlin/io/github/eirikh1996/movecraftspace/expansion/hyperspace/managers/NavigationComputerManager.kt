package io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.NavigationComputer
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.utils.BlockUtils
import io.github.eirikh1996.movecraftspace.utils.InventoryUtils.createItem
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import net.countercraft.movecraft.craft.Craft
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.block.Sign
import org.bukkit.block.data.type.WallSign
import org.bukkit.event.EventHandler
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerInteractEvent
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

object NavigationComputerManager : Iterable<NavigationComputer> {

    private val navigationComputers = HashSet<NavigationComputer>()
    private val navigationComputerInterface = Bukkit.createInventory(null, InventoryType.CHEST, "Navigation computer")
    private val beaconLocations = Bukkit.createInventory(null, InventoryType.CHEST)

    init {

    }

    fun updateGUIs() {
        val contents = navigationComputerInterface.contents
        contents[0] = createItem(if (Settings.IsLegacy) Material.getMaterial("LEGACY_REDSTONE_TORCH_ON")!! else Material.REDSTONE_TORCH, "Beacon locations")
        contents[1] = createItem(Material.GLOWSTONE, "Solar systems")



    }


    @EventHandler
    fun onSignChange(event : SignChangeEvent) {
        if (event.getLine(0) != "[navcomputer]") {
            return
        }
        val sign = event.block.state
        if (sign !is Sign)
            return
        val navigationComputer = getNavigationComputer(sign)
        if (navigationComputer == null) {
            event.player.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Sign is either not attached to a navigation computer structure or attached at the wrong location on the navigation computer structure")
            event.isCancelled = true
            return
        }
        if (!event.player.hasPermission("movecraftspace.navigationcomputer." + navigationComputer.name + ".create")) {
            event.player.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "You don't have permission to create navigation computer " + navigationComputer.name)
            return
        }
        val face = if (Settings.IsLegacy) {
            val signData = sign.data as org.bukkit.material.Sign
            signData.facing
        } else {
            val signData = sign.blockData as WallSign
            signData.facing
        }
        val angle = MSUtils.angleBetweenBlockFaces(navigationComputer.blocks[ImmutableVector.ZERO]!!.facing, face)
        val locs = HashSet<ImmutableVector>()
        for (vec in navigationComputer.blocks.keys) {
            locs.add(ImmutableVector.fromLocation(event.block.location).add(vec.rotate(angle, ImmutableVector.ZERO).add(0,vec.y,0)))

        }
        val SHIFTS = arrayOf(ImmutableVector(1,0,0), ImmutableVector(-1,0,0), ImmutableVector(0,0,1), ImmutableVector(0,0,-1))
        for (vec in locs) {
            for (shift in SHIFTS) {
                val test = vec.add(shift)
                if (locs.contains(test) || test.equals(ImmutableVector.fromLocation(event.block.location)))
                    continue
                if (test.toLocation(event.block.world).block.state !is Sign)
                    continue
                val testSign = test.toLocation(event.block.world).block.state as Sign
                if (testSign.getLine(0) == ChatColor.AQUA.toString() + "Nav computer" && testSign.equals(ChatColor.RED.toString() + navigationComputer.name)) {
                    event.player.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "This navigation computer structure already has a sign attached to it")
                    event.isCancelled = true
                    return
                }
            }
        }

        event.setLine(0, ChatColor.AQUA.toString() + "Nav computer")
        event.setLine(1, ChatColor.RED.toString() + navigationComputer.name)

    }

    fun onSignClick(event : PlayerInteractEvent) {

    }

    fun getNavigationComputersOnCraft(craft: Craft) : Map<Sign, NavigationComputer> {
        val returnMap = HashMap<Sign, NavigationComputer>()
        for (ml in craft.hitBox) {
            val block = ml.toBukkit(craft.w).block
            if (!block.type.name.endsWith("WALL_SIGN"))
                continue
            val sign = block.state as Sign
            val navigationComputer = getNavigationComputer(sign) ?: continue
            returnMap[sign] = navigationComputer
        }
        return returnMap
    }

    fun getNavigationComputer(sign : Sign) : NavigationComputer? {
        if (!sign.block.type.name.endsWith("WALL_SIGN"))
            return null
        var foundNavigationComputer : NavigationComputer? = null
        val face = if (Settings.IsLegacy) {
            val signData = sign.data as org.bukkit.material.Sign
            signData.facing
        } else {
            val signData = sign.blockData as WallSign
            signData.facing
        }
        val iter = navigationComputers.iterator()
        while (iter.hasNext()) {
            val next = iter.next()
            var navigationComputerFound = true
            val angle = MSUtils.angleBetweenBlockFaces(next.blocks[ImmutableVector.ZERO]!!.facing, face)
            for (vec in next.blocks.keys) {
                val hdBlock = next.blocks[vec]!!.rotate(BlockUtils.rotateBlockFace(angle, next.blocks[vec]!!.facing))
                val block = ImmutableVector.fromLocation(sign.location).add(vec.rotate(angle, ImmutableVector.ZERO).add(0,vec.y,0)).toLocation(sign.world).block
                if (block.type.name.endsWith("WALL_SIGN"))
                    continue
                if (!hdBlock.isSimilar(block)) {
                    navigationComputerFound = false
                    break
                }

            }
            if (!navigationComputerFound)
                continue
            foundNavigationComputer = next
            break
        }
        return foundNavigationComputer
    }

    fun getNavigationComputer(name : String): NavigationComputer? {
        for (hd in this) {
            if (!hd.name.equals(name,true))
                continue
            return hd
        }
        return null
    }

    override fun iterator(): Iterator<NavigationComputer> {
        return Collections.unmodifiableSet(navigationComputers).iterator()
    }
}