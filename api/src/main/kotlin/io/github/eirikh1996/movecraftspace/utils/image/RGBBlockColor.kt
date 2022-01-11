package io.github.eirikh1996.movecraftspace.utils.image

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.MSBlock
import org.bukkit.Bukkit
import org.bukkit.Material
import java.awt.Color
import java.io.File
import java.lang.UnsupportedOperationException
import javax.imageio.ImageIO
import kotlin.math.PI
import kotlin.math.sin

object RGBBlockColor {
    val colorBlockMap = HashMap<Color, MSBlock>()

    init {
        if (Settings.IsLegacy) {
            //Concrete
            colorBlockMap[Color(188, 194, 195)] = MSBlock(Material.getMaterial("CONCRETE")!!, 0)
            colorBlockMap[Color(204, 88, 1)] = MSBlock(Material.getMaterial("CONCRETE")!!, 1)
            colorBlockMap[Color(154, 44, 145)] = MSBlock(Material.getMaterial("CONCRETE")!!, 2)
            colorBlockMap[Color(33, 126, 182)] = MSBlock(Material.getMaterial("CONCRETE")!!, 3)
            colorBlockMap[Color(219, 159, 19)] = MSBlock(Material.getMaterial("CONCRETE")!!, 4)
            colorBlockMap[Color(86, 154, 22)] = MSBlock(Material.getMaterial("CONCRETE")!!, 5)
            colorBlockMap[Color(195, 92, 130)] = MSBlock(Material.getMaterial("CONCRETE")!!, 6)
            colorBlockMap[Color(48, 51, 56)] = MSBlock(Material.getMaterial("CONCRETE")!!, 7)
            colorBlockMap[Color(113, 113, 104)] = MSBlock(Material.getMaterial("CONCRETE")!!, 8)
            colorBlockMap[Color(18, 107, 123)] = MSBlock(Material.getMaterial("CONCRETE")!!, 9)
            colorBlockMap[Color(90, 27, 141)] = MSBlock(Material.getMaterial("CONCRETE")!!, 10)
            colorBlockMap[Color(41, 43, 131)] = MSBlock(Material.getMaterial("CONCRETE")!!, 11)
            colorBlockMap[Color(87, 54, 29)] = MSBlock(Material.getMaterial("CONCRETE")!!, 12)
            colorBlockMap[Color(66, 82, 33)] = MSBlock(Material.getMaterial("CONCRETE")!!, 13)
            colorBlockMap[Color(129, 29, 29)] = MSBlock(Material.getMaterial("CONCRETE")!!, 14)
            colorBlockMap[Color(8, 10, 15)] = MSBlock(Material.getMaterial("CONCRETE")!!, 15)
            //Terracotta
            colorBlockMap[Color(189, 159, 145)] = MSBlock(Material.getMaterial("STAINED_CLAY")!!, 0)
            colorBlockMap[Color(146, 75, 33)] = MSBlock(Material.getMaterial("STAINED_CLAY")!!, 1)
            colorBlockMap[Color(133, 77, 96)] = MSBlock(Material.getMaterial("STAINED_CLAY")!!, 2)
            colorBlockMap[Color(104, 99, 126)] = MSBlock(Material.getMaterial("STAINED_CLAY")!!, 3)
            colorBlockMap[Color(172, 124, 33)] = MSBlock(Material.getMaterial("STAINED_CLAY")!!, 4)
            colorBlockMap[Color(95, 106, 47)] = MSBlock(Material.getMaterial("STAINED_CLAY")!!, 5)
            colorBlockMap[Color(146, 70, 71)] = MSBlock(Material.getMaterial("STAINED_CLAY")!!, 6)
            colorBlockMap[Color(51, 37, 31)] = MSBlock(Material.getMaterial("STAINED_CLAY")!!, 7)
            colorBlockMap[Color(123, 97, 89)] = MSBlock(Material.getMaterial("STAINED_CLAY")!!, 8)
            colorBlockMap[Color(78, 83, 82)] = MSBlock(Material.getMaterial("STAINED_CLAY")!!, 9)
            colorBlockMap[Color(108, 64, 78)] = MSBlock(Material.getMaterial("STAINED_CLAY")!!, 10)
            colorBlockMap[Color(66, 52, 81)] = MSBlock(Material.getMaterial("STAINED_CLAY")!!, 11)
            colorBlockMap[Color(72, 46, 33)] = MSBlock(Material.getMaterial("STAINED_CLAY")!!, 12)
            colorBlockMap[Color(68, 75, 38)] = MSBlock(Material.getMaterial("STAINED_CLAY")!!, 13)
            colorBlockMap[Color(129, 55, 42)] = MSBlock(Material.getMaterial("STAINED_CLAY")!!, 14)
            colorBlockMap[Color(34, 20, 15)] = MSBlock(Material.getMaterial("STAINED_CLAY")!!, 15)

            colorBlockMap[Color(121, 82, 68)] = MSBlock(Material.STONE, 1)
            colorBlockMap[Color(144, 97, 79)] = MSBlock(Material.STONE, 2)
            colorBlockMap[Color(217, 217, 217)] = MSBlock(Material.STONE, 3)
            colorBlockMap[Color(176, 177, 177)] = MSBlock(Material.STONE, 4)
            colorBlockMap[Color(128, 128, 127)] = MSBlock(Material.STONE, 5)
            colorBlockMap[Color(107, 111, 109)] = MSBlock(Material.STONE, 6)
        } else {
            //Concrete
            colorBlockMap[Color(188, 194, 195)] = MSBlock(Material.WHITE_CONCRETE)
            colorBlockMap[Color(204, 88, 1)] = MSBlock(Material.ORANGE_CONCRETE)
            colorBlockMap[Color(154, 44, 145)] = MSBlock(Material.MAGENTA_CONCRETE)
            colorBlockMap[Color(33, 126, 182)] = MSBlock(Material.LIGHT_BLUE_CONCRETE)
            colorBlockMap[Color(219, 159, 19)] = MSBlock(Material.YELLOW_CONCRETE)
            colorBlockMap[Color(86, 154, 22)] = MSBlock(Material.LIME_CONCRETE)
            colorBlockMap[Color(195, 92, 130)] = MSBlock(Material.PINK_CONCRETE)
            colorBlockMap[Color(48, 51, 56)] = MSBlock(Material.GRAY_CONCRETE)
            colorBlockMap[Color(113, 113, 104)] = MSBlock(Material.LIGHT_GRAY_CONCRETE)
            colorBlockMap[Color(18, 107, 123)] = MSBlock(Material.CYAN_CONCRETE)
            colorBlockMap[Color(90, 27, 141)] = MSBlock(Material.PURPLE_CONCRETE)
            colorBlockMap[Color(41, 43, 131)] = MSBlock(Material.BLUE_CONCRETE)
            colorBlockMap[Color(87, 54, 29)] = MSBlock(Material.BROWN_CONCRETE)
            colorBlockMap[Color(66, 82, 33)] = MSBlock(Material.GREEN_CONCRETE)
            colorBlockMap[Color(129, 29, 29)] = MSBlock(Material.RED_CONCRETE)
            colorBlockMap[Color(8, 10, 15)] = MSBlock(Material.BLACK_CONCRETE)
            //Terracotta
            colorBlockMap[Color(189, 159, 145)] = MSBlock(Material.WHITE_TERRACOTTA)
            colorBlockMap[Color(146, 75, 33)] = MSBlock(Material.ORANGE_TERRACOTTA)
            colorBlockMap[Color(133, 77, 96)] = MSBlock(Material.MAGENTA_TERRACOTTA)
            colorBlockMap[Color(104, 99, 126)] = MSBlock(Material.LIGHT_BLUE_TERRACOTTA)
            colorBlockMap[Color(172, 124, 33)] = MSBlock(Material.YELLOW_TERRACOTTA)
            colorBlockMap[Color(95, 106, 47)] = MSBlock(Material.LIME_TERRACOTTA)
            colorBlockMap[Color(146, 70, 71)] = MSBlock(Material.PINK_TERRACOTTA)
            colorBlockMap[Color(51, 37, 31)] = MSBlock(Material.GRAY_TERRACOTTA)
            colorBlockMap[Color(123, 97, 89)] = MSBlock(Material.LIGHT_GRAY_TERRACOTTA)
            colorBlockMap[Color(78, 83, 82)] = MSBlock(Material.CYAN_TERRACOTTA)
            colorBlockMap[Color(108, 64, 78)] = MSBlock(Material.PURPLE_TERRACOTTA)
            colorBlockMap[Color(66, 52, 81)] = MSBlock(Material.BLUE_TERRACOTTA)
            colorBlockMap[Color(72, 46, 33)] = MSBlock(Material.BROWN_TERRACOTTA)
            colorBlockMap[Color(68, 75, 38)] = MSBlock(Material.GREEN_TERRACOTTA)
            colorBlockMap[Color(129, 55, 42)] = MSBlock(Material.RED_TERRACOTTA)
            colorBlockMap[Color(34, 20, 15)] = MSBlock(Material.BLACK_TERRACOTTA)

            colorBlockMap[Color(205, 198, 158)] = MSBlock(Material.SANDSTONE)
            colorBlockMap[Color(176, 95, 31)] = MSBlock(Material.RED_SANDSTONE)
            colorBlockMap[Color(139, 84, 59)] = MSBlock(Material.TERRACOTTA)

            colorBlockMap[Color(140, 140, 140)] = MSBlock(Material.COBBLESTONE)

            colorBlockMap[Color(121, 82, 68)] = MSBlock(Material.GRANITE)
            colorBlockMap[Color(144, 97, 79)] = MSBlock(Material.POLISHED_GRANITE)
            colorBlockMap[Color(217, 217, 217)] = MSBlock(Material.DIORITE)
            colorBlockMap[Color(176, 177, 177)] = MSBlock(Material.POLISHED_DIORITE)
            colorBlockMap[Color(128, 128, 127)] = MSBlock(Material.ANDESITE)
            colorBlockMap[Color(107, 111, 109)] = MSBlock(Material.POLISHED_ANDESITE)

            colorBlockMap[Color(233, 233, 233)] = MSBlock(Material.SNOW_BLOCK)
        }
        colorBlockMap[Color(112, 112, 112)] = MSBlock(Material.STONE)
        colorBlockMap[Color(133, 94, 65)] = MSBlock(Material.DIRT)


    }

    fun sphereFromImage(file : File, center : ImmutableVector, radius : Int) : Map<ImmutableVector, MSBlock> {
        val returnMap = HashMap<ImmutableVector, MSBlock>()
        val perimeter = 2 * PI * radius
        val totalArcLength = perimeter / 2

        val image = ImageIO.read(file)
        val diameter = radius * 2
        val heightRatio = totalArcLength / image.height
        val startLoc = center.add(0, radius, 0)
        Bukkit.broadcastMessage(center.toString())
        Bukkit.broadcastMessage(image.height.toString())
        Bukkit.broadcastMessage(image.width.toString())
        Bukkit.broadcastMessage(radius.toString())
        Bukkit.broadcastMessage(startLoc.toString())
        for (y in 0 until image.height) {
            val yArcLength = y * heightRatio
            val angle = yArcLength / radius
            val sine = sin(angle)
            val currentRadius = radius * sine
            val currentPerimeter = 2 * PI * currentRadius
            val widthRatio = currentPerimeter / image.width
            val rotatedYLoc = startLoc.rotate(angle, center, ImmutableVector.Axis.X)
            for (x in 0 until image.width) {
                val xArcLength = widthRatio * x
                val pixel = image.getRGB(x, y)
                val color = Color(pixel)
                val xAngle = xArcLength / radius
                val rotatedXLoc = rotatedYLoc.rotate(xAngle, center, ImmutableVector.Axis.Y)
                returnMap[rotatedXLoc] = getClosestBlock(color)
            }
        }
        return returnMap
    }

    private fun getClosestBlock(color: Color) : MSBlock {
        val rgb = color.rgb
        var lastWhite = Color(255, 255, 255).rgb
        var lastBlack = Color(0, 0, 0).rgb
        for (c in colorBlockMap.keys) {
            if (c.rgb > rgb && c.rgb > lastBlack) {
                lastWhite = c.rgb

            }
            if (c.rgb < rgb && c.rgb < lastWhite) {
                lastBlack = c.rgb
            }

        }
        val closest = colorBlockMap.keys.first { c -> c.rgb in lastBlack..lastWhite }
        return colorBlockMap[closest]!!
    }


}