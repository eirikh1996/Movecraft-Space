package io.github.eirikh1996.movecraftspace.utils

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import java.lang.reflect.Method
import kotlin.math.PI

object BlockUtils {
    val iBlockData : Class<*>
    val craftMagicNumbers : Class<*>
    val craftBlockData : Class<*>
    val getBlock : Method
    val fromData : Method
    val getState : Method
    val toLegacyData : Method
    val faces = arrayOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)

    init {
        val packageName = Bukkit.getServer().javaClass.`package`.name
        val version = packageName.substring(packageName.lastIndexOf(".") + 1)
        iBlockData = Class.forName("net.minecraft." + if (version.split("_")[1].toInt() >= 17) "world.level.block.state"  else "server." + version + ".IBlockData")
        craftMagicNumbers = Class.forName("org.bukkit.craftbukkit." + version + ".util.CraftMagicNumbers")
        craftBlockData = Class.forName("org.bukkit.craftbukkit." + version + ".block.data.CraftBlockData")
        getBlock = craftMagicNumbers.getDeclaredMethod("getBlock", Material::class.java, Byte::class.java)
        fromData = craftBlockData.getDeclaredMethod("fromData", iBlockData)
        getState = craftBlockData.getDeclaredMethod("getState")
        toLegacyData = craftMagicNumbers.getDeclaredMethod("toLegacyData", iBlockData)
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
        if (angle == 0.0)
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