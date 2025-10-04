package io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers

import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.ExpansionSettings
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.Hyperdrive
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector.Axis
import io.github.eirikh1996.movecraftspace.utils.BlockUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import io.github.eirikh1996.movecraftspace.utils.MSUtils.angleBetweenBlockFaces
import net.countercraft.movecraft.MovecraftLocation
import net.countercraft.movecraft.util.MathUtils
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
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

object HyperdriveManager : Listener, Iterable<Hyperdrive> {


    val hyperdrives = HashSet<Hyperdrive>()
    private val zeroVector = MovecraftLocation(0, 0, 0)



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

            val signData = sign.blockData as WallSign
            signData.facing

        val angle = angleBetweenBlockFaces(hyperdrive.blocks[zeroVector]!!.facing, signData.facing)
        val locs = HashSet<MovecraftLocation>()
        for (vec in hyperdrive.blocks.keys) {
            locs.add(MathUtils.bukkit2MovecraftLoc(event.block.location).add(rotate(angle, zeroVector, vec).add(MovecraftLocation(0,vec.y,0))))

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
            val signData = sign.blockData as WallSign
        val iter = hyperdrives.iterator()
        while (iter.hasNext()) {
            val next = iter.next()
            var hyperdriveFound = true
            val angle = angleBetweenBlockFaces(next.blocks[zeroVector]!!.facing, signData.facing)
            for (vec in next.blocks.keys) {
                val hdBlock = next.blocks[vec]!!.rotate(BlockUtils.rotateBlockFace(angle, next.blocks[vec]!!.facing))
                val block = MathUtils.bukkit2MovecraftLoc(sign.location).add(rotate(angle, zeroVector, vec).add(
                    MovecraftLocation(0,vec.y,0))).toBukkit(sign.world).block
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
            val files = hyperdriveDir.listFiles { dir, name -> name.endsWith(".yml", true) }
            if (files != null) {
                for (hdFile in files) {
                    if (hdFile == null)
                        continue
                    val drive = Hyperdrive.loadFromFile(hdFile)
                    hyperdrives.add(drive)
                    drive.save()

                }
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