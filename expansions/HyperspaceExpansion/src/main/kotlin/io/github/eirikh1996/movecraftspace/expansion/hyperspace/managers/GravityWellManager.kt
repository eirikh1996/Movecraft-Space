package io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.HyperspaceExpansion
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.GravityWell
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.Hyperdrive
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.utils.BlockUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import net.countercraft.movecraft.craft.Craft
import net.countercraft.movecraft.craft.CraftManager
import net.countercraft.movecraft.events.CraftDetectEvent
import net.countercraft.movecraft.events.CraftReleaseEvent
import net.countercraft.movecraft.utils.MathUtils
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.block.Sign
import org.bukkit.block.data.type.WallSign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import java.io.File
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

object GravityWellManager : Iterable<GravityWell>, Listener {
    val gravityWells = HashSet<GravityWell>()
    val GRAVITY_WELL_HEADER = ChatColor.AQUA.toString() + "Gravity well"
    val GRAVITY_WELL_ACTIVE_TEXT = ChatColor.GREEN.toString() + "Active"
    val GRAVITY_WELL_STANDBY_TEXT = ChatColor.DARK_RED.toString() + "Standby"

    @EventHandler
    fun onSignChange(event: SignChangeEvent) {
        if (!ChatColor.stripColor(event.getLine(0)).equals("[gravitywell]", true) || !event.block.type.name.endsWith("WALL_SIGN"))
            return
        val gravityWell = getGravityWell(event.block.state as Sign)
        if (gravityWell == null) {
            event.player.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Sign is either not attached to a gravity well structure or attached at the wrong location on the gravity well structure")
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
        val angle = MSUtils.angleBetweenBlockFaces(gravityWell.blocks[ImmutableVector.ZERO]!!.facing, face)
        val locs = HashSet<ImmutableVector>()
        for (vec in gravityWell.blocks.keys) {
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
                if (testSign.getLine(0).equals(ChatColor.AQUA.toString() + "Gravity well") && testSign.equals(ChatColor.RED.toString() + gravityWell.name)) {
                    event.player.sendMessage(COMMAND_PREFIX + ERROR + "This gravity well structure already has a sign attached to it")
                    event.isCancelled = true
                    return
                }
            }
        }
        event.setLine(0, GRAVITY_WELL_HEADER)
        event.setLine(1, ChatColor.RED.toString() + gravityWell.name)
        event.setLine(2, ChatColor.BLUE.toString() + "Range: " + gravityWell.range)
        event.setLine(3, GRAVITY_WELL_STANDBY_TEXT)
    }

    @EventHandler
    fun onInteract(e : PlayerInteractEvent) {
        if (e.action != Action.RIGHT_CLICK_BLOCK)
            return
        val clicked = e.clickedBlock
        if (clicked == null || !clicked.type.name.endsWith("WALL_SIGN"))
            return
        val sign = clicked.state as Sign
        if (!sign.getLine(0).equals(GRAVITY_WELL_HEADER)) {
            return
        }
        val p = e.player
        val foundGravityWell = getGravityWell(sign)
        if (foundGravityWell == null) {
            p.sendMessage(COMMAND_PREFIX + ERROR + "Sign is not part of a gravity well structure")
            return
        }
        if (!CraftManager.getInstance().getCraftsInWorld(sign.world).any { c -> c.hitBox.contains(MathUtils.bukkit2MovecraftLoc(sign.location)) }) {
            p.sendMessage(COMMAND_PREFIX + ERROR + "Gravity well is not part of a piloted craft")
            return
        }
        if (sign.getLine(3).equals(GRAVITY_WELL_STANDBY_TEXT)) {
            sign.setLine(3, GRAVITY_WELL_ACTIVE_TEXT)
            p.sendMessage(COMMAND_PREFIX + "Gravity well " + foundGravityWell.name + " activated")

        } else {
            sign.setLine(3, GRAVITY_WELL_STANDBY_TEXT)
            p.sendMessage(COMMAND_PREFIX + "Gravity well " + foundGravityWell.name + " deactivated")

        }
        sign.update()

    }

    @EventHandler
    fun onDetect(event : CraftDetectEvent) {
        val gravityWellsOnCraft = getGravityWellsOnCraft(event.craft)
        for (entry in gravityWellsOnCraft) {
            if (entry.value.allowedOnCraftTypes.contains(event.craft.type.craftName)) {
                event.failMessage = COMMAND_PREFIX + ERROR + "Gravity well " + entry.value.name + " is not allowed on craft type " + event.craft.type.craftName
                event.isCancelled = true
                return
            }
        }
        if (HyperspaceExpansion.instance.maxGravityWellsOnCraft.contains(event.craft.type.craftName) && gravityWellsOnCraft.size > HyperspaceExpansion.instance.maxGravityWellsOnCraft.get(event.craft.type.craftName)!!) {
            event.failMessage = COMMAND_PREFIX + ERROR + "Craft type " + event.craft.type.craftName + " can have maximum of " + HyperspaceExpansion.instance.maxGravityWellsOnCraft.get(event.craft.type.craftName)!! + " hyperdrives on it"
            event.isCancelled = true
            return
        }

    }

    @EventHandler
    fun onRelease(event :CraftReleaseEvent) {
        for (entry in getGravityWellsOnCraft(event.craft)) {
            entry.key.setLine(3, GRAVITY_WELL_STANDBY_TEXT)
            entry.key.update()
        }
    }

    fun getGravityWellsOnCraft(craft: Craft) : Map<Sign, GravityWell> {
        val returnMap = HashMap<Sign, GravityWell>()
        if (craft.hitBox.isEmpty)
            return returnMap
        for (ml in craft.hitBox) {
            val block = ml.toBukkit(craft.w).block
            if (!block.type.name.endsWith("WALL_SIGN"))
                continue
            val sign = block.state as Sign
            val gravityWell = getGravityWell(sign)
            if (gravityWell == null)
                continue
            returnMap.put(sign, gravityWell)
        }
        return returnMap
    }

    fun getGravityWell(sign : Sign) : GravityWell? {
        if (!sign.block.type.name.endsWith("WALL_SIGN"))
            return null
        var foundGravityWell : GravityWell? = null
        val face = if (Settings.IsLegacy) {
            val signData = sign.data as org.bukkit.material.Sign
            signData.facing
        } else {
            val signData = sign.blockData as WallSign
            signData.facing
        }
        val iter = gravityWells.iterator()
        while (iter.hasNext()) {
            val next = iter.next()
            var gravityWellFound = true
            val angle = MSUtils.angleBetweenBlockFaces(next.blocks[ImmutableVector.ZERO]!!.facing, face)
            for (vec in next.blocks.keys) {
                val hdBlock = next.blocks[vec]!!.rotate(BlockUtils.rotateBlockFace(angle, next.blocks[vec]!!.facing))
                val block = ImmutableVector.fromLocation(sign.location).add(vec.rotate(angle, ImmutableVector.ZERO).add(0,vec.y,0)).toLocation(sign.world).block
                if (block.type.name.endsWith("WALL_SIGN"))
                    continue
                if (!hdBlock.isSimilar(block)) {
                    gravityWellFound = false
                    break
                }

            }
            if (!gravityWellFound)
                continue
            foundGravityWell = next
            break
        }
        return foundGravityWell
    }

    fun craftHasActiveGravityWell(craft: Craft) : Boolean {
        for (ml in craft.hitBox) {
            val b = ml.toBukkit(craft.w).block
            if (!b.type.name.endsWith("WALL_SIGN"))
                continue
            val sign = b.state as Sign
            if (!sign.getLine(0).equals(GRAVITY_WELL_HEADER))
                continue
            val gravityWell = getGravityWell(sign)
            if (gravityWell == null)
                continue
            if (!sign.getLine(3).equals(GRAVITY_WELL_ACTIVE_TEXT))
                continue
            return true
        }
        return false
    }

    fun getActiveGravityWellAt(loc : Location, craftToExclude : Craft? = null): GravityWell? {
        for (craft in CraftManager.getInstance().getCraftsInWorld(loc.world!!)) {
            if (craft == null || craftToExclude == craft)
                continue
            for (ml in craft.hitBox) {
                val b = ml.toBukkit(loc.world).block
                if (!b.type.name.endsWith("WALL_SIGN"))
                    continue
                val sign = b.state as Sign
                if (!sign.getLine(0).equals(GRAVITY_WELL_HEADER))
                    continue
                val gravityWell = getGravityWell(sign)
                if (gravityWell == null)
                    continue
                if (sign.location.distance(loc) > gravityWell.range)
                    continue
                if (!sign.getLine(3).equals(GRAVITY_WELL_ACTIVE_TEXT))
                    continue
                return gravityWell
            }
        }
        return null
    }

    fun getGravityWell(name : String) : GravityWell? {
        return gravityWells.find { gw -> gw.name.equals(name, true) }
    }

    fun add(gravityWell: GravityWell) {
        gravityWells.add(gravityWell)
        gravityWell.save()
    }

    fun loadGravityWells() {

        val gravityWellDir = File(HyperspaceExpansion.instance.dataFolder, "gravitywells")
        if (gravityWellDir.exists()) {
            for (hdFile in gravityWellDir.listFiles { dir, name -> name.endsWith(".yml", true) }) {
                if (hdFile == null)
                    continue
                val well = GravityWell.loadFromFile(hdFile)
                gravityWells.add(well)
                well.save()

            }
        }

        HyperspaceExpansion.instance.logMessage(Expansion.LogMessageType.INFO, "Loaded " + gravityWells.size + " gravity wells")
    }
    /**
     * Returns an iterator over the elements of this object.
     */
    override fun iterator(): Iterator<GravityWell> {
        return Collections.unmodifiableSet(gravityWells).iterator()
    }
}