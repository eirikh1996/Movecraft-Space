package io.github.eirikh1996.movecraftspace.expansion.selection

import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import net.countercraft.movecraft.MovecraftLocation
import org.bukkit.World

data class Selection(var minX : Int, var maxX : Int, var minY : Int, var maxY : Int, var minZ : Int, var maxZ : Int, val world: World) : Iterable<MovecraftLocation> {
    val size : Int get() { return xLength * yLength * zLength }

    val xLength : Int get() = maxX - minX + 1
    val yLength : Int get() = maxY - minY + 1
    val zLength : Int get() = maxZ - minZ + 1
    val center : MovecraftLocation get() = MovecraftLocation(minX + (xLength / 2), minY + (yLength / 2), minZ + (zLength / 2))
    /**
     * Returns an iterator over the elements of this object.
     */
    override fun iterator(): Iterator<MovecraftLocation> {
        return object : Iterator<MovecraftLocation> {
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
            override fun next(): MovecraftLocation {
                val output = MovecraftLocation(x, y, z)
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