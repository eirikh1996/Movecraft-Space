package io.github.eirikh1996.movecraftspace.expansion.hyperspace.sign

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.HyperspaceExpansion
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.HyperdriveManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.HyperspaceManager
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import net.countercraft.movecraft.craft.CraftManager
import net.countercraft.movecraft.events.CraftDetectEvent
import net.countercraft.movecraft.utils.MathUtils
import org.bukkit.ChatColor
import org.bukkit.block.Sign
import org.bukkit.block.data.type.WallSign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.player.PlayerInteractEvent

object HyperspaceSign : Listener {
    val HEADER = "§bHyperspace"
    @EventHandler
    fun onChange(e : SignChangeEvent) {
        if (!ChatColor.stripColor(e.getLine(0))!!.equals("[hyperspace]"))
            return
        e.setLine(0, HEADER)
    }

    fun onDetect(e : CraftDetectEvent) {
        val craft = e.craft
        var range = 0
        HyperdriveManager.getHyperdrivesOnCraft(craft).forEach { t, u -> range += u.maxRange }
        for (ml in craft.hitBox) {
            val b = ml.toBukkit(craft.w).block
            if (!b.type.name.endsWith("WALL_SIGN"))
                continue
            val sign = b.state as Sign
            if (!sign.getLine(0).equals(HEADER)) {
                continue
            }
            sign.setLine(1, "§9Range: §6" + range)
        }
    }
    @EventHandler
    fun onInteract(e : PlayerInteractEvent) {
        if (e.action != Action.RIGHT_CLICK_BLOCK)
            return
        if (!e.clickedBlock!!.type.name.endsWith("SIGN") && !e.clickedBlock!!.type.name.equals("SIGN_POST")) {
            return
        }
        val sign = e.clickedBlock!!.state as Sign
        if (!sign.getLine(0).equals("§bHyperspace")) {
            return
        }
        if (!e.player.hasPermission("movecraftspace.hyperspace.sign")) {
            e.player.sendMessage(COMMAND_PREFIX + ERROR + "You don't have permission to use the Hyperspace sign")
            return
        }
        val craft = CraftManager.getInstance().getCraftByPlayer(e.player)
        if (craft == null) {
            e.player.sendMessage(COMMAND_PREFIX + ERROR + "You are not piloting a craft")
            return
        }
        if (!craft.hitBox.contains(MathUtils.bukkit2MovecraftLoc(sign.location))) {
            e.player.sendMessage(COMMAND_PREFIX + ERROR + "Sign is not on a piloted craft")
            return
        }
        for (pl in PlanetCollection) {
            if (pl.space.equals(craft.w)) {
                continue
            }
            e.player.sendMessage(COMMAND_PREFIX + ERROR + "You can only use hyperspace travel in space worlds")
            return
        }
        if (!HyperspaceExpansion.instance.allowedCraftTypesForHyperspaceSign.contains(craft.type)) {
            e.player.sendMessage(COMMAND_PREFIX + ERROR + "Craft type " + craft.type.craftName + " is not allowed for hyperspace travel using sign")
            return
        }
        val hyperdrivesOnCraft = HyperdriveManager.getHyperdrivesOnCraft(craft)
        if (hyperdrivesOnCraft.isEmpty()) {
            e.player.sendMessage(COMMAND_PREFIX + ERROR + "There are no hyperdrives on this craft")
            return
        }
        var range = 0
        for (entry in hyperdrivesOnCraft) {
            val hyperdrive = entry.value
            range += hyperdrive.maxRange
        }
        val face = if (Settings.IsLegacy) {
            val signData = sign.data as org.bukkit.material.Sign
            signData.facing
        } else {
            if (sign.blockData is WallSign) {
                val signData = sign.blockData as WallSign
                signData.facing
            } else {
                val signData = sign.blockData as org.bukkit.block.data.type.Sign
                signData.rotation
            }
        }
        val direction = face.oppositeFace.direction.clone()
        direction.multiply(range)
        val origin = sign.location
        val destination = sign.location.clone().add(direction)
        HyperspaceManager.scheduleHyperspaceTravel(craft, origin, destination)
    }
}