package io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers

import io.github.eirikh1996.movecraftspace.expansion.hyperspace.ExpansionSettings
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.GravityWell
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import net.countercraft.movecraft.craft.Craft
import net.countercraft.movecraft.craft.CraftManager
import net.countercraft.movecraft.events.CraftDetectEvent
import net.countercraft.movecraft.events.CraftReleaseEvent
import org.bukkit.Location
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler

class Movecraft7GravityWellProcessor : GravityWellManager.GravityWellProcessor<Craft>() {
    override fun craftHasActiveGravityWell(craft: Craft) : Boolean {
        for (ml in craft.hitBox) {
            val b = ml.toBukkit(craft.w).block
            if (!b.type.name.endsWith("WALL_SIGN"))
                continue
            val sign = b.state as Sign
            if (sign.getLine(0) != GravityWellManager.GRAVITY_WELL_HEADER)
                continue
            val gravityWell = GravityWellManager.getGravityWell(sign) ?: continue
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
                if (!sign.getLine(0).equals(GravityWellManager.GRAVITY_WELL_HEADER))
                    continue
                val gravityWell = GravityWellManager.getGravityWell(sign) ?: continue
                if (sign.location.distance(loc) > gravityWell.range)
                    continue
                if (!sign.getLine(3).equals(GravityWellManager.GRAVITY_WELL_ACTIVE_TEXT))
                    continue
                return gravityWell
            }
        }
        return null
    }

    @EventHandler
    fun onDetect(event : CraftDetectEvent) {
        val gravityWellsOnCraft = GravityWellManager.getGravityWellsOnCraft(event.craft.hitBox.asSet(), event.craft.w)
        for (entry in gravityWellsOnCraft) {
            if (entry.value.allowedOnCraftTypes.contains(event.craft.type.craftName)) {
                event.failMessage = MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Gravity well " + entry.value.name + " is not allowed on craft type " + event.craft.type.craftName
                event.isCancelled = true
                return
            }
        }
        if (ExpansionSettings.maxGravityWellsOnCraft.contains(event.craft.type.craftName) && gravityWellsOnCraft.size > ExpansionSettings.maxGravityWellsOnCraft[event.craft.type.craftName]!!) {
            event.failMessage = MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Craft type " + event.craft.type.craftName + " can have maximum of " + ExpansionSettings.maxGravityWellsOnCraft[event.craft.type.craftName]!! + " hyperdrives on it"
            event.isCancelled = true
            return
        }

    }

    @EventHandler
    fun onRelease(event : CraftReleaseEvent) {
        for (entry in GravityWellManager.getGravityWellsOnCraft(event.craft.hitBox.asSet(), event.craft.w)) {
            entry.key.setLine(3, GravityWellManager.GRAVITY_WELL_STANDBY_TEXT)
            entry.key.update()
        }
    }
}