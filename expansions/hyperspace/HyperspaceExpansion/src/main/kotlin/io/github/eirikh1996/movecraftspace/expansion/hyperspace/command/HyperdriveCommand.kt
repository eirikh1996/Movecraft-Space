package io.github.eirikh1996.movecraftspace.expansion.hyperspace.command

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.HyperdriveManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.Hyperdrive
import io.github.eirikh1996.movecraftspace.expansion.selection.SelectionManager
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_NO_PERMISSION
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import io.github.eirikh1996.movecraftspace.utils.MSUtils.MUST_BE_PLAYER
import net.countercraft.movecraft.craft.CraftManager
import org.bukkit.block.Sign
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryHolder

object HyperdriveCommand : TabExecutor {
    override fun onTabComplete(sender: CommandSender, command : Command, p2: String, args: Array<out String>): MutableList<String> {
        val tabCompletions = ArrayList<String>()
        if (args.size <= 1) {
            tabCompletions.add("save")
            tabCompletions.add("paste")

        }
        if (args[0].equals("paste", true)) {
           HyperdriveManager.forEach { hd -> tabCompletions.add(hd.name) }
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
        if (!command.name.equals("hyperdrive", true)) {
            return false
        }
        if (sender !is Player) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + MUST_BE_PLAYER)
            return true
        }
        if (!sender.hasPermission("movecraftspace.command.hyperdrive")) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + COMMAND_NO_PERMISSION)
            return true
        }
        if (args[0].equals("save", true)) {
            if (args.size < 4) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Invalid syntax. Correct syntax is /hyperdrive save <name> <range> <warmupTime> [allowed craft types, separated by space]")
                return true
            }
            val sel = SelectionManager.selections[sender.uniqueId]
            if (sel == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "You have no selection. Make one with your selection wand, which is a " + Settings.SelectionWand)
                return true
            }
            var foundSigns = 0
            var foundInventoryBlocks = 0
            var signLoc = ImmutableVector(0, 0, 0)
            for (vec in sel) {
                val block = vec.toLocation(sel.world).block
                if (block.state is InventoryHolder)
                    foundInventoryBlocks++
                if (block.state is Sign) {
                    signLoc = ImmutableVector.fromLocation(block.location)
                    foundSigns++
                }
            }
            if (foundSigns <= 0) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "There are no signs on the hyperdrive structure")
                return true
            }
            if (foundSigns > 1) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Hyperdrive structures can have maximum one sign")
                return true
            }
            if (foundInventoryBlocks <= 0) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "There are no inventory blocks on the hyperdrive structure")
                return true
            }
            val hyperdrive : Hyperdrive
            val maxRange : Int
            val warmupTime : Int
            try {
                maxRange = args[2].toInt()
            } catch (e : NumberFormatException) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + args[2] + " is not a number")
                return true
            }
            try {
                warmupTime = args[3].toInt()
            } catch (e : NumberFormatException) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + args[3] + " is not a number")
                return true
            }
            val allowedOnCraftTypes = HashSet<String>()
            if (args.size > 4) {
                allowedOnCraftTypes.addAll(args.copyOfRange(4, args.size - 1))
            }
            hyperdrive = Hyperdrive(args[1], maxRange, warmupTime, allowedOnCraftTypes)
            hyperdrive.copy(sel, signLoc)

            val iterator = HyperdriveManager.hyperdrives.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (next.name != hyperdrive.name)
                    continue

                iterator.remove()
            }
            sender.sendMessage(COMMAND_PREFIX + "Sucessfully saved selected structure for hyperdrive " + hyperdrive.name)
            HyperdriveManager.add(hyperdrive)


        } else if (args[0].equals("paste", true)) {
            if (args.size <= 1) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "You need so specify the name of hyperdrive to show structure")
                return true
            }
            val hd = HyperdriveManager.getHyperdrive(args[1])
            if (hd == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Hyperdrive " + args[1] + " does not exist")
                return true
            }
            hd.paste(sender.location)
        }
        return true
    }
}