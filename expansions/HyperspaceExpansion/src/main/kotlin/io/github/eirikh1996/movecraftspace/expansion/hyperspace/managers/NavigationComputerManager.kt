package io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.HyperspaceExpansion
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.NavigationComputer
import io.github.eirikh1996.movecraftspace.objects.StarCollection
import io.github.eirikh1996.movecraftspace.utils.BlockUtils
import io.github.eirikh1996.movecraftspace.utils.InventoryUtils.createItem
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import net.countercraft.movecraft.MovecraftLocation
import net.countercraft.movecraft.craft.Craft
import net.countercraft.movecraft.craft.PilotedCraft
import net.countercraft.movecraft.craft.PlayerCraft
import net.countercraft.movecraft.util.MathUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
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
import org.bukkit.event.block.Action
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.*

object NavigationComputerManager : Iterable<NavigationComputer>, Listener, BukkitRunnable() {

    val navigationComputers = HashMap<String, NavigationComputer>()
    private val navigationComputerInterface = Bukkit.createInventory(null, 9, Component.text("Navigation computer"))
    private val beaconLocations = ArrayList<Inventory>()
    private val solarSystems = ArrayList<Inventory>()//Bukkit.createInventory(null, InventoryType.CHEST, Component.text("Solar systems"))

    private val zeroVector = MovecraftLocation(0, 0, 0)

    private val FIRST_LINE = Component.text("Nav computer", NamedTextColor.AQUA)
    private val BEACON_LOCATIONS_COMPONENT = Component.text("Beacon locations")
    private val SOLAR_SYSTEMS_COMPONENT = Component.text("Solar systems")
    private val PREVIOUS_PAGE = Component.text("Previous page")
    private val NEXT_PAGE = Component.text("Next page")
    private val MAIN_MENU = Component.text("Main menu")

    init {
        updateGUIs()
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
        //Navigation computer interface
        val nciContents = navigationComputerInterface.contents
        nciContents[0] = createItem(Material.REDSTONE_TORCH, BEACON_LOCATIONS_COMPONENT)
        nciContents[1] = createItem(Material.GLOWSTONE, SOLAR_SYSTEMS_COMPONENT)
        navigationComputerInterface.contents = nciContents
        //Beacon location interface

        val blItems = ArrayList<ItemStack>()
        for (beacon in HyperspaceManager.beaconLocations) {
            blItems.add(createItem(
                Material.REDSTONE_TORCH,
                Component.text(beacon.originName + " - " + beacon.destinationName),
                Component.text("Space world: " + beacon.origin.world.name),
                Component.text("Location: x" + beacon.origin.blockX + ", z" + beacon.origin.blockZ))
            )
            blItems.add(createItem(
                Material.REDSTONE_TORCH,
                Component.text(beacon.destinationName + " - " + beacon.originName),
                Component.text("Space world: " + beacon.destination.world.name),
                Component.text("Location: x" + beacon.destination.blockX + ", z" + beacon.destination.blockZ))
            )
        }
        blItems.sortedBy { item -> item.itemMeta.customName().toString() }
        var max = blItems.size
        var blItemsList = ArrayList<List<ItemStack>>()
        var pageItems = ArrayList<ItemStack>()
        blItems.forEach { item ->
            pageItems.add(item)
            if (pageItems.size == min(max, 45)) {
                blItemsList.add(pageItems)
                max -= 45
                pageItems = ArrayList()
            }
        }

        var totalPages = max(ceil(blItems.size / 45.0).toInt(), 1)
        for (p in 1..totalPages) {
            val inv = Bukkit.createInventory(null, 54, Component.text("Beacon locations, page $p/$totalPages"))
            val contents = inv.contents
            if (blItemsList.isNotEmpty()) {
                val items = blItemsList[p - 1]
                for (i in items.indices) {
                    contents[i] = items[i]
                }
            }
            for (i in 45..53) {
                when (i) {
                    46 -> { //previous
                        contents[i] = createItem(
                            if (p == 1)
                                Material.RED_STAINED_GLASS_PANE
                            else
                                Material.GREEN_STAINED_GLASS_PANE,
                            PREVIOUS_PAGE,
                        )
                    }
                    49 -> { //main menu
                        contents[i] = createItem(
                            Material.BLUE_STAINED_GLASS_PANE,
                            MAIN_MENU
                        )
                    }
                    52 -> { //next
                        contents[i] = createItem(
                            if (p == 1 && totalPages == 1 || p == totalPages)
                                Material.RED_STAINED_GLASS_PANE
                            else
                                Material.GREEN_STAINED_GLASS_PANE,
                            NEXT_PAGE,
                        )
                    }
                    else -> {
                        contents[i] = ItemStack(Material.BLACK_STAINED_GLASS_PANE)
                    }
                }
            }
            inv.contents = contents
            beaconLocations.add(inv)
        }

        //Solar system
        val ssItems = ArrayList<ItemStack>()
        for (star in StarCollection) {
            ssItems.add(
                createItem(
                    Material.GLOWSTONE,
                    Component.text(star.name),
                    Component.text("Space world: ${star.space.name}"),
                    Component.text("x: ${star.loc.x}"),
                    Component.text("y: ${star.loc.y}"),
                    Component.text("z: ${star.loc.z}")
                )
            )
        }
        val ssItemsList = ArrayList<List<ItemStack>>()
        pageItems = ArrayList<ItemStack>()
        ssItems.sortedBy { s -> s.itemMeta.customName()?.let { PlainTextComponentSerializer.plainText().serialize(it) } }
        max = ssItems.size
        ssItems.forEach { s ->
            pageItems.add(s)
            if (pageItems.size == min(max, 45)) {
                ssItemsList.add(pageItems)
                pageItems = ArrayList()
                max -= 45
            }
        }
        totalPages = max(ceil(ssItems.size / 45.0).toInt(), 1)
        for (p in 1..totalPages) {
            val inv = Bukkit.createInventory(null, 54, Component.text("Solar systems, page $p/$totalPages"))
            val contents = inv.contents
            if (ssItemsList.isNotEmpty()) {
                val items = ssItemsList[p - 1]
                for (i in items.indices) {
                    contents[i] = items[i]
                }
            }
            for (i in 45..53) {
                when (i) {
                    46 -> { //previous
                        contents[i] = createItem(
                            if (p == 1)
                                Material.RED_STAINED_GLASS_PANE
                            else
                                Material.GREEN_STAINED_GLASS_PANE,
                            PREVIOUS_PAGE,
                        )
                    }
                    49 -> { //main menu
                        contents[i] = createItem(
                            Material.BLUE_STAINED_GLASS_PANE,
                            MAIN_MENU
                        )
                    }
                    52 -> { //next
                        contents[i] = createItem(
                            if (p == 1 && totalPages == 1 || p == totalPages)
                                Material.RED_STAINED_GLASS_PANE
                            else
                                Material.GREEN_STAINED_GLASS_PANE,
                            NEXT_PAGE,
                        )
                    }
                    else -> {
                        contents[i] = ItemStack(Material.BLACK_STAINED_GLASS_PANE)
                    }
                }
            }
            inv.contents = contents
            solarSystems.add(inv)
        }



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
        val angle = MSUtils.angleBetweenBlockFaces(navigationComputer.blocks[zeroVector]!!.facing, signData.facing)
        val locs = HashSet<MovecraftLocation>()
        for (vec in navigationComputer.blocks.keys) {
            locs.add(MathUtils.bukkit2MovecraftLoc(event.block.location).add(rotate(angle, zeroVector, vec).add(
                MovecraftLocation(0,vec.y,0))))

        }
        val SHIFTS = arrayOf(MovecraftLocation(1,0,0), MovecraftLocation(-1,0,0), MovecraftLocation(0,0,1), MovecraftLocation(0,0,-1))
        for (vec in locs) {
            for (shift in SHIFTS) {
                val test = vec.add(shift)
                if (locs.contains(test) || test.equals(MathUtils.bukkit2MovecraftLoc(event.block.location)))
                    continue
                if (test.toBukkit(event.block.world).block.state !is Sign)
                    continue
                val testSign = test.toBukkit(event.block.world).block.state as Sign
                if (testSign.getLine(0) == ChatColor.AQUA.toString() + "Nav computer" && testSign.equals(ChatColor.RED.toString() + navigationComputer.name)) {
                    event.player.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "This navigation computer structure already has a sign attached to it")
                    event.isCancelled = true
                    return
                }
            }
        }
        event.line(0, FIRST_LINE)
        event.line(1, Component.text(navigationComputer.name, NamedTextColor.RED))
    }

    @EventHandler
    fun onSignClick(event : PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK)
            return
        val clicked = event.clickedBlock ?: return
        if (clicked.blockData !is WallSign)
            return
        val sign = clicked.state as Sign
        if (sign.getSide(Side.FRONT).line(0) != FIRST_LINE) {
            return
        }
        event.isCancelled = true
        val player = event.player
        player.openInventory(navigationComputerInterface)
    }

    @EventHandler
    fun onInventoryClick(event : InventoryClickEvent) {
        val inv = event.inventory
        if (inv != navigationComputerInterface && !solarSystems.contains(inv) && !beaconLocations.contains(inv))
            return
        event.isCancelled = true
        val clickedItem = event.currentItem ?: return
        val meta = clickedItem.itemMeta
        if (!meta.hasCustomName())
            return
        val view = event.view
        val title = PlainTextComponentSerializer.plainText().serialize(view.title())

        if (meta.customName() == BEACON_LOCATIONS_COMPONENT) {
            event.whoClicked.openInventory(beaconLocations[0])
        } else if (meta.customName() == SOLAR_SYSTEMS_COMPONENT) {
            event.whoClicked.openInventory(solarSystems[0])
        } else if (title.startsWith("Beacon locations, page ")) {//Beacon locations
            val fraction = title.replace("Beacon locations, page ", "")
            val parts = fraction.split("/")
            val pageNo = parts[0].toInt()
            val page =
            if (clickedItem.type == Material.GREEN_STAINED_GLASS_PANE && clickedItem.itemMeta.customName() == PREVIOUS_PAGE) {
                beaconLocations[pageNo - 2]
            } else if (clickedItem.type == Material.GREEN_STAINED_GLASS_PANE && clickedItem.itemMeta.customName() == NEXT_PAGE) {
                beaconLocations[pageNo]
            } else if (clickedItem.type == Material.BLUE_STAINED_GLASS_PANE && clickedItem.itemMeta.customName() == MAIN_MENU) {
                navigationComputerInterface
            } else {
                null
            }
            if (page == null)
                return
            event.whoClicked.openInventory(page)
        } else if (title.startsWith("Solar systems")) {
            val fraction = title.replace("Solar systems, page ", "")
            val parts = fraction.split("/")
            val pageNo = parts[0].toInt()
            val page =
                if (clickedItem.type == Material.GREEN_STAINED_GLASS_PANE && clickedItem.itemMeta.customName() == PREVIOUS_PAGE) {
                    solarSystems[pageNo - 2]
                } else if (clickedItem.type == Material.GREEN_STAINED_GLASS_PANE && clickedItem.itemMeta.customName() == NEXT_PAGE) {
                    solarSystems[pageNo]
                } else if (clickedItem.type == Material.BLUE_STAINED_GLASS_PANE && clickedItem.itemMeta.customName() == MAIN_MENU) {
                    navigationComputerInterface
                } else {
                    null
                }
            if (page == null)
                return
            event.whoClicked.openInventory(page)
        }

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
            val angle = MSUtils.angleBetweenBlockFaces(computer.blocks[zeroVector]!!.facing, face)
            for (vec in computer.blocks.keys) {
                val hdBlock = computer.blocks[vec]!!.rotate(BlockUtils.rotateBlockFace(angle, computer.blocks[vec]!!.facing))
                val block = MathUtils.bukkit2MovecraftLoc(sign.location).add(rotate(angle, zeroVector, vec).add(
                    MovecraftLocation(0,vec.y,0))).toBukkit(sign.world).block
                val similar = hdBlock.isSimilar(block)
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

    override fun run() {
        object : BukkitRunnable() {
            override fun run() {
                updateGUIs()
            }
        }.runTask(HyperspaceExpansion.instance.plugin)
    }

    private fun rotate(angle : Double, origin : MovecraftLocation, offset : MovecraftLocation) : MovecraftLocation {
        val toRotate = offset.subtract(origin)
        val cos = cos(angle)
        val sin = sin(angle)
        val x : Int = round(toRotate.x * cos + toRotate.z * -sin).toInt()
        val y : Int = 0
        val z : Int = round(toRotate.x * sin + toRotate.z * cos).toInt()

        return MovecraftLocation(x, y, z).add(origin)
    }
}