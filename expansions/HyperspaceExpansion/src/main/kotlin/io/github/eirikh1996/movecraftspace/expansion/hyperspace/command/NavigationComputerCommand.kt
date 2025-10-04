package io.github.eirikh1996.movecraftspace.expansion.hyperspace.command

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.HyperdriveManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.NavigationComputerManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.NavigationComputer
import io.github.eirikh1996.movecraftspace.expansion.selection.SelectionManager
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_NO_PERMISSION
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import io.github.eirikh1996.movecraftspace.utils.MSUtils.MUST_BE_PLAYER
import net.countercraft.movecraft.MovecraftLocation
import net.countercraft.movecraft.craft.CraftManager
import net.countercraft.movecraft.craft.type.CraftType
import net.countercraft.movecraft.util.MathUtils
import org.bukkit.block.Sign
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryHolder

object NavigationComputerCommand : TabExecutor {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): MutableList<String>? {
        val tabCompletions = ArrayList<String>()
        if (args.size <= 1) {
            tabCompletions.add("save")
            tabCompletions.add("paste")

        }
        if (args[0].equals("paste", true)) {
            NavigationComputerManager.forEach { hd -> tabCompletions.add(hd.name) }
        }
        if (args[0].equals("save", true) && args.size >= 4) {
            CraftManager.getInstance().craftTypes.forEach { type -> tabCompletions.add(type.getStringProperty(CraftType.NAME)) }
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

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!command.name.equals("navigationcomputer", true)) {
            return false
        }
        if (sender !is Player) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + MUST_BE_PLAYER)
            return true
        }
        if (!sender.hasPermission("movecraftspace.command.navigationcomputer")) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + COMMAND_NO_PERMISSION)
            return true
        }
        if (args[0].equals("save", true)) {
            if (args.size < 4) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Invalid syntax. Correct syntax is /navigationcomputer save <name> <range> [allowed craft types, separated by space]")
                return true
            }
            val sel = SelectionManager.selections[sender.uniqueId]
            if (sel == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "You have no selection. Make one with your selection wand, which is a " + Settings.SelectionWand)
                return true
            }
            var foundSigns = 0
            var signLoc = MovecraftLocation(0, 0, 0)
            for (vec in sel) {
                val block = vec.toBukkit(sel.world).block
                if (block.state is Sign) {
                    signLoc = MathUtils.bukkit2MovecraftLoc(block.location)
                    foundSigns++
                }
            }
            if (foundSigns <= 0) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "There are no signs on the navigation computer structure")
                return true
            }
            if (foundSigns > 1) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Navigation computer structures can have maximum one sign")
                return true
            }
            val navigationcomputer : NavigationComputer
            val maxRange : Int
            try {
                maxRange = args[2].toInt()
            } catch (e : NumberFormatException) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + args[2] + " is not a number")
                return true
            }
            val allowedOnCraftTypes = HashSet<String>()
            if (args.size > 4) {
                allowedOnCraftTypes.addAll(args.copyOfRange(4, args.size - 1))
            }
            navigationcomputer = NavigationComputer(args[1], maxRange, allowedOnCraftTypes)
            navigationcomputer.copy(sel, signLoc)

            val original = NavigationComputerManager.getNavigationComputer(navigationcomputer.name)
            if (original != null) {
                NavigationComputerManager.navigationComputers.remove(original.name)
            }
            NavigationComputerManager.add(navigationcomputer)
            sender.sendMessage(COMMAND_PREFIX + "Sucessfully saved selected structure for navigation computer " + navigationcomputer.name)
            NavigationComputerManager.add(navigationcomputer)


        } else if (args[0].equals("paste", true)) {
            if (args.size <= 1) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "You need so specify the name of navigation computer to show structure")
                return true
            }
            val nc = NavigationComputerManager.getNavigationComputer(args[1])
            if (nc == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Navigation computer " + args[1] + " does not exist")
                return true
            }
            nc.paste(sender.location)
        }
        return true
    }
}