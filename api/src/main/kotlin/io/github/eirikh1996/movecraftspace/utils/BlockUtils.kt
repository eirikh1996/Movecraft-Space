package io.github.eirikh1996.movecraftspace.utils

import io.github.eirikh1996.movecraftspace.Settings
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import java.lang.Exception
import java.lang.reflect.Method
import kotlin.math.PI

object BlockUtils {
    val iBlockData : Class<*>
    val craftMagicNumbers : Class<*>
    lateinit var craftBlockData : Class<*>
    lateinit var getBlock : Method
    lateinit var fromData : Method
    lateinit var getState : Method
    lateinit var toLegacyData : Method
    val faces = arrayOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)

    init {
        val packageName = Bukkit.getServer().javaClass.`package`.name
        val version = packageName.substring(packageName.lastIndexOf(".") + 1)
        iBlockData = Class.forName("net.minecraft." + (if (Settings.IsV1_17) "world.level.block.state"  else "server." + version) + ".IBlockData")
        craftMagicNumbers = Class.forName("org.bukkit.craftbukkit." + version + ".util.CraftMagicNumbers")
        try {
            if (!Settings.IsLegacy) {
                craftBlockData = Class.forName("org.bukkit.craftbukkit." + version + ".block.data.CraftBlockData")
                fromData = craftBlockData.getDeclaredMethod("fromData", iBlockData)
                getState = craftBlockData.getDeclaredMethod("getState")
                getBlock = craftMagicNumbers.getDeclaredMethod("getBlock", Material::class.java, Byte::class.java)
                toLegacyData = craftMagicNumbers.getDeclaredMethod("toLegacyData", iBlockData)
            }
        } catch (e : Exception) {}


    }

    fun blockDataFromMaterialandLegacyData(type : Material, data : Byte) : BlockData {
        val nmsBlockData = getBlock.invoke(null, type, data)
        return fromData.invoke(null, nmsBlockData) as BlockData
    }

    fun byteDataFromBlockData(data : BlockData): Byte {
        val iBlockData = getState.invoke(data)
        return toLegacyData.invoke(null, iBlockData) as Byte
    }

    fun rotateBlockFace(angle : Double, startPoint : BlockFace) : BlockFace {
        if (angle == 0.0 || startPoint == BlockFace.SELF)
            return startPoint
        var index = faces.indexOf(startPoint)
        var varAngle = angle
        while (varAngle != 0.0) {
            varAngle -= (PI / 2)
            index++
            if (index > 3)
                index = 0
            if (varAngle >= (2 * PI))
                varAngle = 0.0
        }
        return faces[index]
    }
}