package io.github.eirikh1996.movecraftspace.utils

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.Planet
import net.countercraft.movecraft.Movecraft
import net.countercraft.movecraft.MovecraftLocation
import net.countercraft.movecraft.WorldHandler
import net.countercraft.movecraft.craft.Craft
import net.countercraft.movecraft.utils.BitmapHitBox
import net.countercraft.movecraft.utils.HitBox
import net.countercraft.movecraft.utils.MathUtils
import org.bukkit.*
import org.bukkit.Bukkit.getConsoleSender
import org.bukkit.block.BlockFace
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.lang.Exception
import java.lang.reflect.Method
import java.util.concurrent.Future
import javax.imageio.ImageIO
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

object MSUtils {
    private var setBlockFast : Method? = null
    lateinit var plugin : Plugin
    init {
        try {
            setBlockFast = WorldHandler::class.java.getDeclaredMethod("setBlockFast", Location::class.java, Material::class.java, Any::class.java)
        } catch (e : Exception) {
            setBlockFast = null
        }
    }
    fun randomCoords(minX : Int, maxX : Int, minY : Int, maxY : Int, minZ : Int, maxZ : Int) : IntArray {

        return intArrayOf(Random.nextInt(minX, maxX), Random.nextInt(minY, maxY), Random.nextInt(minZ, maxZ))
    }

    fun hitboxObstructed(craft: Craft, planet: Planet?, destWorld : World, diff : MovecraftLocation) : Boolean {
        var obstructed = false;
        for (ml in craft.hitBox) {
            val destHitBoxLoc = ml.add(diff)
            val destHitBoxLocType = destHitBoxLoc.toBukkit(destWorld).block.type
            if (planet != null && planet.space.equals(destWorld) && planet.contains(ml.toBukkit(destWorld))) {
                obstructed = true
                break
            }
            if (!destHitBoxLocType.name.endsWith("AIR") && !craft.type.passthroughBlocks.contains(destHitBoxLocType)) {
                obstructed = true
                break
            }
            if (!MathUtils.withinWorldBorder(destWorld, destHitBoxLoc)) {
                obstructed = true
                break
            }

        }
        return obstructed
    }

    fun displayTitle() {
        val version = plugin.description.version
        getConsoleSender().sendMessage(" __      __                                                  ___    _            ____")
        getConsoleSender().sendMessage("|  \\    /  |                                               / __\\  | |          / __ \\ ")
        getConsoleSender().sendMessage("|   \\  /   |   $version                                     | |    | |         / /  \\_\\ ")
        getConsoleSender().sendMessage("|    \\/    |  __   _  _     ___    __   __  ___    ____   _| |_  _| |_   ___  | |__    _  ___   ____      __    ___")
        getConsoleSender().sendMessage("| |\\    /| | /  \\ | || |  / _ \\  /  \\ |  |/ _ \\  /___ \\ |_   _||_   _| |___| \\\\__ \\  | |/ _ \\ /___  \\   /  \\  / _ \\ ")
        getConsoleSender().sendMessage("| | \\  / | |/ /\\ \\| || | / /_\\_\\/ /\\_\\|    / \\_\\ ____| |  | |    | |             \\ \\ |   / \\ | ____| | / /\\_\\/ /_\\_\\ ")
        getConsoleSender().sendMessage("| |  \\/  | || || || || || |   __| | __|   /     /  __  |  | |    | |         __   | ||   | | |/  __  | | | __| |  __" )
        getConsoleSender().sendMessage("| |      | |\\ \\/ /\\ \\/ / \\ \\_/ /\\ \\/ /|  |      | |__| |  | |    | |__       \\ \\_/ / |   \\_/ || |__| | \\ \\/ /\\ \\_/ /")
        getConsoleSender().sendMessage("|_|      |_| \\__/  \\__/   \\___/  \\__/ |  |      \\______|  |_|    \\____/       \\___/  | |\\___/ \\______|  \\__/  \\___/ ")
        getConsoleSender().sendMessage("                                                                                     | |")
        getConsoleSender().sendMessage("                                                                                     | |")
        getConsoleSender().sendMessage("                                                                                     |_|")

    }



    fun setBlock(loc : Location, type : Material, data : Any) {
        val wh = Movecraft.getInstance().worldHandler
        if (setBlockFast == null) {
            wh.setBlockFast(loc, type, data as Byte)
        } else {
            setBlockFast!!.invoke(wh, loc, type, data)
        }

    }

    fun createSphere(radius : Int, center : ImmutableVector) : Set<ImmutableVector> {
        val minX = center.x - radius
        val maxX = center.x + radius
        val minZ = center.z - radius
        val maxZ = center.z + radius
        val radiusSquared = radius * radius
        val sphere = HashSet<ImmutableVector>()
        for (y in 0..255) {
            for (x in minX..maxX) {
                for (z in minZ..maxZ) {
                    val vec = ImmutableVector(x, y, z)
                    if (abs(vec.distanceSquared(center) - radiusSquared) > radius)
                        continue
                    sphere.add(vec)
                }
            }
        }
        return sphere

    }

    fun angleBetweenBlockFaces(first : BlockFace, second : BlockFace) : Double {
        val faces = arrayOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)
        if (!faces.contains(first) || !faces.contains(second)) {
            throw IllegalArgumentException("Blockfaces can only be NORTH, EAST, SOUTH and WEST")
        }
        if (first == second)
            return 0.0
        var index = faces.indexOf(first)
        var multiplier = 0
        while (true) {
            val face = faces[index]
            if (face == second) {
                break
            }
            index++
            if (index > 3)
                index = 0
            multiplier++
        }
        return (PI / 2) * multiplier
    }

    val COMMAND_PREFIX = "§5[§9Movecraft-Space§5]§r "
    val ERROR = "§4Error: "
    val WARNING = "§6Warning: "
    val COMMAND_NO_PERMISSION = "You have no permission to execute this command!"
    val MUST_BE_PLAYER = "You must be a player to use this command"
}