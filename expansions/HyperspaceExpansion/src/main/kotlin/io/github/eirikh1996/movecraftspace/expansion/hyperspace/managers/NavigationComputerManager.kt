package io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.NavigationComputer
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.utils.BlockUtils
import io.github.eirikh1996.movecraftspace.utils.InventoryUtils.createItem
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import net.countercraft.movecraft.craft.Craft
import net.countercraft.movecraft.craft.PilotedCraft
import net.countercraft.movecraft.craft.PlayerCraft
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.Sign
import org.bukkit.block.data.Directional
import org.bukkit.block.data.type.WallSign
import org.bukkit.block.sign.Side
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

object NavigationComputerManager : Iterable<NavigationComputer>, Listener {

    val navigationComputers = HashMap<String, NavigationComputer>()
    private val navigationComputerInterface = Bukkit.createInventory(null, InventoryType.CHEST, "Navigation computer")
    private val beaconLocations = ArrayList<Inventory>()
    private val solarSystems = Bukkit.createInventory(null, InventoryType.CHEST, "Solar systems")

    init {
        //Navigation computer interface
        val nciContents = navigationComputerInterface.contents
        nciContents[0] = createItem(Material.REDSTONE_TORCH, "Beacon locations")
        nciContents[1] = createItem(Material.GLOWSTONE, "Solar systems")

        //Beacon location interface

        val blItems = ArrayList<ItemStack>()
        for (beacon in HyperspaceManager.beaconLocations) {
            blItems.add(createItem(
                Material.REDSTONE_TORCH,
                beacon.originName + " - " + beacon.destinationName,
                "Space world: " + beacon.origin.world.name,
                "Location: x" + beacon.origin.blockX + ", z" + beacon.origin.blockZ)
            )
            blItems.add(createItem(
                Material.REDSTONE_TORCH,
                beacon.destinationName + " - " + beacon.originName,
                "Space world: " + beacon.destination.world.name,
                "Location: x" + beacon.destination.blockX + ", z" + beacon.destination.blockZ)
            )
        }
        var max = blItems.size
        var count = 1
        var totalCount = 1
        var totalPages = Math.ceil(blItems.size / 45.0).toInt()
        var page = 1
        var inv = Bukkit.createInventory(null, InventoryType.CHEST, "Beacon locations")
        for (item in blItems) {
            inv.contents[count - 1] = item
            if (count == 45 || totalCount >= max) {
                count = 1
                for (i in 45..53) {
                    when (i) {
                        46 -> { //previous
                            inv.contents[i] = createItem(
                                if (page == 1 || totalPages == 1)
                                    Material.RED_STAINED_GLASS_PANE
                                else
                                    Material.GREEN_STAINED_GLASS_PANE,
                                "Previous page",
                            )
                        }
                        52 -> { //next
                            inv.contents[i] = createItem(
                                if (page == 1 && totalPages == 1 || page == totalPages)
                                    Material.RED_STAINED_GLASS_PANE
                                else
                                    Material.GREEN_STAINED_GLASS_PANE,
                                "Next page",
                            )
                        }
                        else -> {
                            inv.contents[i] = ItemStack(Material.BLACK_STAINED_GLASS_PANE)
                        }
                    }
                }
                beaconLocations.add(inv)
                if (totalCount >= max) {
                    break
                }
                page++
                continue
            }
            count++

        }

        //Solar system
        max = blItems.size
        count = 1
        totalCount = 1
        totalPages = Math.ceil(blItems.size / 45.0).toInt()
        page = 1
        inv = Bukkit.createInventory(null, InventoryType.CHEST, "Beacon locations")
    }

    private fun closestMultipleOf9(number : Int) : Int {
        return when (number) {
            in 0..9 -> 9 //1
            in 10..18 -> 18 //2
            in 19..27 -> 27 //3
            in 28..36 -> 36 //4
            in 37..45 -> 45 //5
            else -> 54
        }
    }

    fun updateGUIs() {




    }


    @EventHandler
    fun onSignChange(event : SignChangeEvent) {
        if (event.getLine(0) != "[navcomputer]") {
            return
        }
        val sign = event.block.state
        val wallSign = sign.blockData
        if (sign !is Sign || wallSign !is WallSign)
            return
        val navigationComputer = getNavigationComputer(sign)
        Bukkit.broadcastMessage(wallSign.facing.name)
        if (navigationComputer == null) {
            event.player.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Sign is either not attached to a navigation computer structure or attached at the wrong location on the navigation computer structure")
            event.isCancelled = true
            return
        }
        if (!event.player.hasPermission("movecraftspace.navigationcomputer." + navigationComputer.name + ".create")) {
            event.player.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "You don't have permission to create navigation computer " + navigationComputer.name)
            return
        }
            val signData = sign.blockData as WallSign
        val angle = MSUtils.angleBetweenBlockFaces(navigationComputer.blocks[ImmutableVector.ZERO]!!.facing, signData.facing)
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

    fun maxRange(craft: PilotedCraft) : Int {
        val navComputersOnCraft = getNavigationComputersOnCraft(craft)
        var range = 0
        for (entry in navComputersOnCraft) {
            val navComputer = entry.value
            if (range > navComputer.maxRange) {
                continue
            }
            range = navComputer.maxRange
        }
        return range
    }

    fun getNavigationComputersOnCraft(craft: Craft) : Map<Sign, NavigationComputer> {
        val returnMap = HashMap<Sign, NavigationComputer>()
        for (ml in craft.hitBox) {
            val block = ml.toBukkit(craft.world).block
            if (!block.type.name.endsWith("WALL_SIGN"))
                continue
            val sign = block.state as Sign
            val navigationComputer = getNavigationComputer(sign) ?: continue
            returnMap[sign] = navigationComputer
        }
        return returnMap
    }

    fun getNavigationComputer(sign : Sign) : NavigationComputer? {
        val data = sign.blockData
        if (!sign.block.type.name.endsWith("WALL_SIGN"))
            return null
        var foundNavigationComputer : NavigationComputer? = null
        val face = (data as Directional).facing
        val iter = navigationComputers.keys.iterator()
        while (iter.hasNext()) {
            val next = iter.next()
            var navigationComputerFound = true
            val computer = navigationComputers[next] ?: continue
            val angle = MSUtils.angleBetweenBlockFaces(computer.blocks[ImmutableVector.ZERO]!!.facing, face)
            for (vec in computer.blocks.keys) {
                val hdBlock = computer.blocks[vec]!!.rotate(BlockUtils.rotateBlockFace(angle, computer.blocks[vec]!!.facing))
                val block = ImmutableVector.fromLocation(sign.location).add(vec.rotate(angle, ImmutableVector.ZERO).add(0,vec.y,0)).toLocation(sign.world).block
                val similar = hdBlock.isSimilar(block)
                for (player in sign.world.players) {
                    player.sendBlockChange(block.location, Bukkit.createBlockData(Material.GLOWSTONE))
                }
                if (block.type.name.endsWith("WALL_SIGN"))
                    continue
                if (!similar) {
                    navigationComputerFound = false
                    break
                }

            }
            if (!navigationComputerFound)
                continue
            foundNavigationComputer = computer
            break
        }
        return foundNavigationComputer
    }

    fun getNavigationComputer(name : String): NavigationComputer? {
        return navigationComputers[name]
    }

    fun add(navigationComputer: NavigationComputer) : Boolean {
        navigationComputer.save()
        return navigationComputers.put(navigationComputer.name, navigationComputer) != null
    }

    override fun iterator(): Iterator<NavigationComputer> {
        return Collections.unmodifiableSet(navigationComputers.values.toSet()).iterator()
    }

    fun loadNavigationComputers() {
        val ex = ExpansionManager.getExpansion("HyperspaceExpansion")!!
        val navComputerDir = File(ex.dataFolder, "navigationcomputers")
        if (navComputerDir.exists()) {
            val files = navComputerDir.listFiles { dir, name -> name.endsWith(".yml", true) }
            if (files != null) {
                for (file in files) {
                    if (file == null)
                        continue
                    val computer = NavigationComputer.loadFromFile(file)
                    navigationComputers.put(computer.name, computer)
                    computer.save()
                }
            }
            ex.logMessage(Expansion.LogMessageType.INFO, "Loaded " + navigationComputers.size + " navigation computers")
        }
    }
}