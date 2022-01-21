package io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers

import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.GravityWell
import net.countercraft.movecraft.craft.Craft
import net.countercraft.movecraft.craft.CraftManager
import org.bukkit.Location
import org.bukkit.block.Sign

class Movecraft8GravityWellProcessor : GravityWellManager.GravityWellProcessor<Craft>() {
    override fun craftHasActiveGravityWell(craft: Craft) : Boolean {
        for (ml in craft.hitBox) {
            val b = ml.toBukkit(craft.world).block
            if (!b.type.name.endsWith("WALL_SIGN"))
                continue
            val sign = b.state as Sign
            if (sign.getLine(0) != GravityWellManager.GRAVITY_WELL_HEADER)
                continue
            GravityWellManager.getGravityWell(sign) ?: continue
            if (sign.getLine(3) != GravityWellManager.GRAVITY_WELL_ACTIVE_TEXT)
                continue
            return true
        }
        return false
    }

    override fun getActiveGravityWellAt(loc : Location, craftToExclude : Craft?): GravityWell? {
        for (craft in CraftManager.getInstance().getCraftsInWorld(loc.world!!)) {
            if (craft == null || craftToExclude == craft)
                continue
            for (ml in craft.hitBox) {
                val b = ml.toBukkit(loc.world).block
                if (!b.type.name.endsWith("WALL_SIGN"))
                    continue
                val sign = b.state as Sign
                if (sign.getLine(0) != GravityWellManager.GRAVITY_WELL_HEADER)
                    continue
                val gravityWell = GravityWellManager.getGravityWell(sign) ?: continue
                if (sign.location.distance(loc) > gravityWell.range)
                    continue
                if (sign.getLine(3) != GravityWellManager.GRAVITY_WELL_ACTIVE_TEXT)
                    continue
                return gravityWell
            }
        }
        return null
    }
}