package io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.GravityWell
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.Hyperdrive
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.utils.BlockUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import net.countercraft.movecraft.craft.Craft
import org.bukkit.block.Sign
import org.bukkit.block.data.type.WallSign

object GravityWellManager : Iterable<GravityWell> {
    val gravityWells = HashSet<GravityWell>()

    fun getGravityWellsOnCraft(craft: Craft) : Map<Sign, GravityWell> {
        val returnMap = HashMap<Sign, GravityWell>()
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
            var hyperdriveFound = true
            val angle = MSUtils.angleBetweenBlockFaces(next.blocks[ImmutableVector.ZERO]!!.facing, face)
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
            foundGravityWell = next
            break
        }
        return foundGravityWell
    }
    /**
     * Returns an iterator over the elements of this object.
     */
    override fun iterator(): Iterator<GravityWell> {
        TODO("Not yet implemented")
    }
}