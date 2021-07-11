package io.github.eirikh1996.movecraftspace.expansion.hyperspace.command

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.HyperspaceExpansion
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.GravityWellManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.HyperdriveManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.GravityWell
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.Hyperdrive
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.MSBlock
import io.github.eirikh1996.movecraftspace.expansion.selection.SelectionManager
import io.github.eirikh1996.movecraftspace.utils.BlockUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import net.countercraft.movecraft.craft.CraftManager
import net.countercraft.movecraft.craft.CraftType
import org.bukkit.block.Block
import org.bukkit.block.Sign
import org.bukkit.block.data.BlockData
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

object GravityWellCommand : TabExecutor {
    override fun onTabComplete(sender: CommandSender, command : Command, p2: String, args: Array<out String>): MutableList<String> {
        val tabCompletions = ArrayList<String>()
        if (args.size <= 1) {
            tabCompletions.add("wand")
            tabCompletions.add("save")
            tabCompletions.add("paste")

        }
        if (args[0].equals("paste", true)) {
            GravityWellManager.forEach( { hd -> tabCompletions.add(hd.name) })
        }
        if (args[0].equals("save", true) && args.size >= 4) {
            CraftManager.getInstance().craftTypes.forEach { type -> tabCompletions.add(type.craftName) }
        }
        if (args.size == 0)
            return tabCompletions

        val completions = ArrayList<String>()
        for (tabcompletion in tabCompletions) {
            if (!tabcompletion.startsWith(args[args.size - 1]))
                continue
            completions.add(tabcompletion)
        }
        return completions
    }

    override fun onCommand(sender: CommandSender, command: Command, p2: String, args: Array<out String>): Boolean {
        if (!command.name.equals("gravitywell", true)) {
            return false
        }
        if (sender !is Player) {
            sender.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + MSUtils.MUST_BE_PLAYER)
            return true
        }
        if (!sender.hasPermission("movecraftspace.command.gravitywell")) {
            sender.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + MSUtils.COMMAND_NO_PERMISSION)
            return true
        }
        if (args[0].equals("save", true)) {
            if (args.size < 3) {
                sender.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Invalid syntax. Correct syntax is /gravitywell save <name> <range> [allowed craft types, separated by space]")
                return true
            }
            val sel = SelectionManager.selections[sender.uniqueId]
            if (sel == null) {
                sender.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "You have no selection. Make one with your gravity well selection wand, which is a " + Settings.SelectionWand)
                return true
            }
            var foundSigns = 0
            var signLoc = ImmutableVector(0, 0, 0)
            for (vec in sel) {
                val block = vec.toLocation(sel.world).block
                if (block.state is Sign) {
                    signLoc = ImmutableVector.fromLocation(block.location)
                    foundSigns++
                }
            }
            if (foundSigns <= 0) {
                sender.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "There are no signs on the gravity well structure")
                return true
            }
            if (foundSigns > 1) {
                sender.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Gravity well structures can have maximum one sign")
                return true
            }
            val gravityWell : GravityWell
            try {
                val allowedOnCraftTypes = HashSet<String>()
                if (args.size > 4) {
                    allowedOnCraftTypes.addAll(args.copyOfRange(4, args.size - 1))
                }
                gravityWell = GravityWell(args[1], args[2].toInt(), allowedOnCraftTypes)
                gravityWell.copy(sel, signLoc)
            } catch (e : NumberFormatException) {
                sender.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + args[2] + " is not a number")
                return true
            }
            val iterator = GravityWellManager.gravityWells.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (next.name != gravityWell.name)
                    continue

                iterator.remove()
            }
            sender.sendMessage(MSUtils.COMMAND_PREFIX + "Sucessfully saved selected structure for gravity well " + gravityWell.name)
            GravityWellManager.add(gravityWell)


        } else if (args[0].equals("paste", true)) {
            if (args.size <= 1) {
                sender.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "You need so specify the name of hyperdrive to show structure")
                return true
            }
            val hd = GravityWellManager.getGravityWell(args[1])
            if (hd == null) {
                sender.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Hyperdrive " + args[1] + " does not exist")
                return true
            }
            hd.paste(sender.location)
        }
        return true
    }

}