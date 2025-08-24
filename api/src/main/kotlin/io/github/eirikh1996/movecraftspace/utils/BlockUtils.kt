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
    val faces = arrayOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)

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