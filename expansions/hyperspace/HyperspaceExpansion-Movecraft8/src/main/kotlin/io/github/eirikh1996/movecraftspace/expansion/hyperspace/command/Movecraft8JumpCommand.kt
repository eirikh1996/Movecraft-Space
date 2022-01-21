package io.github.eirikh1996.movecraftspace.expansion.hyperspace.command

import io.github.eirikh1996.movecraftspace.expansion.hyperspace.ExpansionSettings
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.Movecraft8HyperspaceProcessor
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.HyperdriveManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.HyperspaceManager
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import io.github.eirikh1996.movecraftspace.utils.MSUtils.MUST_BE_PLAYER
import io.github.eirikh1996.movecraftspace.utils.MSUtils.WARNING
import net.countercraft.movecraft.craft.CraftManager
import net.countercraft.movecraft.craft.type.CraftType
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.Vector

object Movecraft8JumpCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean {
        if (!cmd.name.equals("jump", true))
            return false
        if (sender !is Player) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + MUST_BE_PLAYER)
            return true
        }
        if (args.size < 2) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + "Correct command syntax is /jump <targetX> <targetZ>")
            return true
        }
        val craft = CraftManager.getInstance().getCraftByPlayer(sender)
        if (craft == null) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + "You are not piloting a craft")
            return true
        }
        val hyperdrivesOnCraft = HyperdriveManager.getHyperdrivesOnCraft(craft.hitBox.asSet(), craft.w)
        if (hyperdrivesOnCraft.isEmpty()) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + "There are no hyperdrives on this craft")
            return true
        }
        val craftTypeName = craft.type.getStringProperty(CraftType.NAME)
        if (!ExpansionSettings.allowedCraftTypesForJumpCommand.contains(craftTypeName)) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + "Craft type " + craftTypeName + " is not allowed for hyperspace travel using jump command")
            return true
        }
        if (!PlanetCollection.any { p -> p.space == craft.world }) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + "You can only use hyperspace travel in a space world")
            return true
        }
        if (craft.hitBox.isEmpty) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + "Hitbox on your craft is empty")
            return true
        }
        val targX : Int
        val targZ : Int
        try {
            targX = args[0].toInt()
        } catch (e : NumberFormatException) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + args[0] + " is not a number")
            return true
        }
        try {
            targZ = args[1].toInt()
        } catch (e : NumberFormatException) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + args[1] + " is not a number")
            return true
        }
        val midpoint = craft.hitBox.midPoint
        if (targX == midpoint.x && targZ == midpoint.z) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + "Cannot jump to the same coordinates as your craft")
            return true
        }
        val dx = targX - midpoint.x
        val dz = targZ - midpoint.z
        val range = HyperdriveManager.getMaxRange(craft.hitBox.asSet(), craft.w)
        val distanceVec = Vector(dx, 0, dz)
        if (distanceVec.length() > range) {
            val origDistance = distanceVec.length()
            distanceVec.normalize()
            distanceVec.multiply(range)
            sender.sendMessage(COMMAND_PREFIX + WARNING + "Travel distance to x" + targX + " and z" + targZ + " is " + origDistance + ", but the range of the craft's hyperdrive" + (if (hyperdrivesOnCraft.size > 1) "s " else " ") + (if (hyperdrivesOnCraft.size > 1) "are " else "is ") + range + " blocks. New target is x" + (midpoint.x + distanceVec.blockX) + " z" + (midpoint.z + distanceVec.blockZ))
        }
        val origin = midpoint.toBukkit(craft.w)
        (HyperspaceManager.processor as Movecraft8HyperspaceProcessor).scheduleHyperspaceTravel(craft, origin, origin.clone().add(distanceVec))
        return true
    }
}