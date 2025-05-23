package io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.ExpansionSettings
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.Hyperdrive
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.utils.BlockUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import io.github.eirikh1996.movecraftspace.utils.MSUtils.angleBetweenBlockFaces
import net.countercraft.movecraft.MovecraftLocation
import org.bukkit.ChatColor
import org.bukkit.World
import org.bukkit.block.Sign
import org.bukkit.block.data.type.WallSign
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent
import java.io.File
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

object HyperdriveManager : Listener, Iterable<Hyperdrive> {


    val hyperdrives = HashSet<Hyperdrive>()



    @EventHandler
    fun onSignChange(event: SignChangeEvent) {
        if (!ChatColor.stripColor(event.getLine(0)).equals("[hyperdrive]", true) || !event.block.type.name.endsWith("WALL_SIGN"))
            return
        val hyperdrive = getHyperdrive(event.block.state as Sign)
        if (hyperdrive == null) {
            event.player.sendMessage(COMMAND_PREFIX + ERROR + "Sign is either not attached to a hyperdrive structure or attached at the wrong location on the hyperdrive structure")
            event.isCancelled = true
            return
        }
        if (!event.player.hasPermission("movecraftspace.hyperdrive." + hyperdrive.name + ".create")) {
            event.player.sendMessage(COMMAND_PREFIX + ERROR + "You don't have permission to create hyperdrive " + hyperdrive.name)
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

    fun onCraftDetect(pilot : Player, craftName : String, hitBox: Set<MovecraftLocation>, craftWorld: World) : String {
        val hyperdrivesOnCraft = getHyperdrivesOnCraft(hitBox, craftWorld)
        val max = ExpansionSettings.maxHyperdrivesOnCraft.getOrDefault(craftName, 0)
        if (hyperdrivesOnCraft.size > max) {
            return COMMAND_PREFIX + ERROR + "Craft type " + craftName + " can have maximum of " + max + " hyperdrives."
        }
        for (hd in hyperdrivesOnCraft.values) {
            if (hd.allowedOnCraftTypes.isNotEmpty() && !hd.allowedOnCraftTypes.contains(craftName)) {
                return COMMAND_PREFIX + ERROR + "Hyperdrive " + hd.name + " is not allowed to use on craft type " + craftName
            }
        }
        return ""
    }

    fun getMaxRange(hitBox: Set<MovecraftLocation>, world: World) : Int {
        var range = 0
        for (e in getHyperdrivesOnCraft(hitBox, world)) {
            range += e.value.maxRange
        }
        return range
    }

    fun getHyperdrivesOnCraft(hitBox : Set<MovecraftLocation>, world: World) : Map<Sign, Hyperdrive> {
        val returnMap = HashMap<Sign, Hyperdrive>()
        for (ml in hitBox) {
            val block = ml.toBukkit(world).block
            if (!block.type.name.endsWith("WALL_SIGN"))
                continue
            val sign = block.state as Sign
            if (sign.getLine(0) != "§bHyperdrive")
                continue
            val hyperdrive = getHyperdrive(sign) ?: continue
            returnMap[sign] = hyperdrive
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
                if (!hdBlock.isSimilar(block)) {
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
            if (!hd.name.equals(name,true))
                continue
            return hd
        }
        return null
    }

    fun loadHyperdrives() {
        val ex = ExpansionManager.getExpansion("HyperspaceExpansion")!!

        val hyperdriveDir = File(ex.dataFolder, "hyperdrives")
        if (hyperdriveDir.exists()) {
            for (hdFile in hyperdriveDir.listFiles { dir, name -> name.endsWith(".yml", true) }) {
                if (hdFile == null)
                    continue
                val drive = Hyperdrive.loadFromFile(hdFile)
                hyperdrives.add(drive)
                drive.save()

            }
        }

        ex.logMessage(Expansion.LogMessageType.INFO, "Loaded " + hyperdrives.size + "hyperdrives")
    }

    fun add(hyperdrive: Hyperdrive) : Boolean {
        hyperdrive.save()
        return hyperdrives.add(hyperdrive)
    }



    /**
     * Returns an iterator over the elements of this object.
     */
    override fun iterator(): Iterator<Hyperdrive> {
        return Collections.unmodifiableCollection(hyperdrives).iterator()
    }
}