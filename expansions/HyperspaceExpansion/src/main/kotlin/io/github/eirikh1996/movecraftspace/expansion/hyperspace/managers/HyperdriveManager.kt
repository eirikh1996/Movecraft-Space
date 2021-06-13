package io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.HyperspaceExpansion
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.Hyperdrive
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.utils.BlockUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import io.github.eirikh1996.movecraftspace.utils.MSUtils.angleBetweenBlockFaces
import net.countercraft.movecraft.craft.Craft
import net.countercraft.movecraft.events.CraftDetectEvent
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Sign
import org.bukkit.block.data.type.WallSign
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import java.io.File
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

object HyperdriveManager : Listener, Iterable<Hyperdrive> {

    val selections = HashMap<UUID, Selection>()
    val hyperdrives = HashSet<Hyperdrive>()

    @EventHandler
    fun onWandUse(event : PlayerInteractEvent) {
        val player = event.player
        if (!player.hasPermission("movecraftspace.hyperspace.wand") || event.item == null || event.item!!.type != HyperspaceExpansion.instance.hyperdriveSelectionWand)
            return
        event.isCancelled = true
        val action = event.action
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val clicked = event.clickedBlock?.location!!
        if (action == Action.LEFT_CLICK_BLOCK) {
            player.sendMessage(COMMAND_PREFIX + "Started selection at (" + clicked.blockX + ", " + clicked.blockY + ", " + clicked.blockZ + "). Right click with your selection wand to encompass selection")
            if (selections.containsKey(player.uniqueId)) {
                val selection = selections[player.uniqueId]!!
                selection.minX = clicked.blockX
                selection.maxX = clicked.blockX
                selection.maxY = clicked.blockY
                selection.minY = clicked.blockY
                selection.minZ = clicked.blockZ
                selection.maxZ = clicked.blockZ
                selections.put(player.uniqueId, selection)
                return
            }
            selections.put(player.uniqueId, Selection(clicked.blockX, clicked.blockX, clicked.blockY, clicked.blockY, clicked.blockZ, clicked.blockZ, clicked.world!!))
            return
        }
        val selection = selections[player.uniqueId]
        if (selection == null) {
            player.sendMessage(COMMAND_PREFIX + "There is no selection available. Start by left-click the block of the hyperdrive structure, then right click to encompass the structure")
            return
        }
        var selChanged = false
        if (selection.minX > clicked.blockX) {
            selChanged = true
            selection.minX = clicked.blockX
        }
        if (selection.maxX < clicked.blockX) {
            selChanged = true
            selection.maxX = clicked.blockX
        }
        if (selection.minY > clicked.blockY) {
            selChanged = true
            selection.minY = clicked.blockY
        }
        if (selection.maxY < clicked.blockY) {
            selChanged = true
            selection.maxY = clicked.blockY
        }
        if (selection.minZ > clicked.blockZ) {
            selChanged = true
            selection.minZ = clicked.blockZ
        }
        if (selection.maxZ < clicked.blockZ) {
            selChanged = true
            selection.maxZ = clicked.blockZ
        }
        if (selChanged) {
            player.sendMessage(COMMAND_PREFIX + "Encompassed selection to (" + clicked.blockX + ", " + clicked.blockY + ", " + clicked.blockZ + "). Size " + selection.size)
            selections.put(player.uniqueId, selection)
        }

    }

    @EventHandler
    fun onSignChange(event: SignChangeEvent) {
        if (!ChatColor.stripColor(event.getLine(0)).equals("[hyperdrive]") || !event.block.type.name.endsWith("WALL_SIGN"))
            return
        val hyperdrive = getHyperdrive(event.block.state as Sign)
        if (hyperdrive == null) {
            event.player.sendMessage(COMMAND_PREFIX + ERROR + "Sign is either not attached to a hyperdrive structure or attached at the wrong location on the hyperdrive structure")
            event.isCancelled = true
            return
        }
        val sign = event.block.state as Sign
        val face = if (Settings.IsLegacy) {
            val signData = sign.data as org.bukkit.material.Sign
            signData.facing
        } else {
            val signData = sign.blockData as WallSign
            signData.facing
        }
        val angle = angleBetweenBlockFaces(hyperdrive.blocks[ImmutableVector.ZERO]!!.facing, face)
        val locs = HashSet<ImmutableVector>()
        for (vec in hyperdrive.blocks.keys) {
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
                if (testSign.getLine(0).equals(ChatColor.AQUA.toString() + "Hyperdrive") && testSign.equals(ChatColor.RED.toString() + hyperdrive.name)) {
                    event.player.sendMessage(COMMAND_PREFIX + ERROR + "This hyperdrive structure already has a sign attached to it")
                    event.isCancelled = true
                    return
                }
            }
        }
        event.setLine(0, ChatColor.AQUA.toString() + "Hyperdrive")
        event.setLine(1, ChatColor.RED.toString() + hyperdrive.name)
        event.setLine(3, ChatColor.GOLD.toString() + "Standby")
    }

    @EventHandler
    fun onDetect(event : CraftDetectEvent) {
        val hyperdrivesOnCraft = getHyperdrivesOnCraft(event.craft)
        val max = HyperspaceExpansion.instance.maxHyperdrivesOnCraft.getOrDefault(event.craft.type, 0)
        if (hyperdrivesOnCraft.size > max) {
            event.failMessage = COMMAND_PREFIX + ERROR + "Craft type " + event.craft.type.craftName + " can have maximum of " + max + " hyperdrives."
            event.isCancelled = true
            return
        }
        for (hd in hyperdrivesOnCraft.values) {
            if (!hd.allowedOnCraftTypes.isEmpty() && !hd.allowedOnCraftTypes.contains(event.craft.type)) {
                event.failMessage = COMMAND_PREFIX + ERROR + "Hyperdrive " + hd.name + " is not allowed to use on craft type " + event.craft.type.craftName
                event.isCancelled
                return
            }
        }


    }

    fun getHyperdrivesOnCraft(craft: Craft) : Map<Sign, Hyperdrive> {
        val returnMap = HashMap<Sign, Hyperdrive>()
        for (ml in craft.hitBox) {
            val block = ml.toBukkit(craft.w).block
            if (!block.type.name.endsWith("WALL_SIGN"))
                continue
            val sign = block.state as Sign
            val hyperdrive = getHyperdrive(sign)
            if (hyperdrive == null)
                continue
            returnMap.put(sign, hyperdrive)
        }
        return returnMap
    }

    fun getHyperdrive(sign : Sign) : Hyperdrive? {
        if (!sign.block.type.name.endsWith("WALL_SIGN"))
            return null
        var foundHyperdrive : Hyperdrive? = null
        val face = if (Settings.IsLegacy) {
            val signData = sign.data as org.bukkit.material.Sign
            signData.facing
        } else {
            val signData = sign.blockData as WallSign
            signData.facing
        }
        val iter = hyperdrives.iterator()
        while (iter.hasNext()) {
            val next = iter.next()
            var hyperdriveFound = true
            val angle = angleBetweenBlockFaces(next.blocks[ImmutableVector.ZERO]!!.facing, face)
            for (vec in next.blocks.keys) {
                val hdBlock = next.blocks[vec]!!.rotate(BlockUtils.rotateBlockFace(angle, next.blocks[vec]!!.facing))
                val block = ImmutableVector.fromLocation(sign.location).add(vec.rotate(angle, ImmutableVector.ZERO).add(0,vec.y,0)).toLocation(sign.world).block
                if (block.type.name.endsWith("WALL_SIGN"))
                    continue
                if (block.type != hdBlock.type || block.data != hdBlock.data) {
                    hyperdriveFound = false
                    break
                }

            }
            if (!hyperdriveFound)
                continue
            foundHyperdrive = next
            break
        }
        return foundHyperdrive
    }

    fun getHyperdrive(name : String): Hyperdrive? {
        for (hd in this) {
            if (hd.name.equals(name,true))
                continue
            return hd
        }
        return null
    }

    fun loadHyperdrives() {

        val hyperdriveDir = File(HyperspaceExpansion.instance.dataFolder, "hyperdrives")
        if (hyperdriveDir.exists()) {
            for (hdFile in hyperdriveDir.listFiles { dir, name -> name.endsWith(".yml", true) }) {
                if (hdFile == null)
                    continue
                hyperdrives.add(Hyperdrive.loadFromFile(hdFile))

            }
        }

        HyperspaceExpansion.instance.logMessage(Expansion.LogMessageType.INFO, "Loaded " + hyperdrives.size + " hyperdrives")
    }

    fun add(hyperdrive: Hyperdrive) : Boolean {
        hyperdrive.save()
        return hyperdrives.add(hyperdrive)
    }

    data class Selection(var minX : Int, var maxX : Int, var minY : Int, var maxY : Int, var minZ : Int, var maxZ : Int, val world: World) : Iterable<ImmutableVector> {
        val size : Int get() { return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1) }

        /**
         * Returns an iterator over the elements of this object.
         */
        override fun iterator(): Iterator<ImmutableVector> {
            return object : Iterator<ImmutableVector> {
                var x = minX
                var y = minY
                var z = minZ
                /**
                 * Returns `true` if the iteration has more elements.
                 */
                override fun hasNext(): Boolean {
                    return z <= maxZ
                }

                /**
                 * Returns the next element in the iteration.
                 */
                override fun next(): ImmutableVector {
                    val output = ImmutableVector(x, y, z)
                    x++
                    if (x > maxX) {
                        x = minX
                        y++
                    }
                    if (y > maxY) {
                        y = minY
                        z++
                    }
                    return output
                }

            }
        }
    }

    /**
     * Returns an iterator over the elements of this object.
     */
    override fun iterator(): Iterator<Hyperdrive> {
        return Collections.unmodifiableCollection(hyperdrives).iterator()
    }
}