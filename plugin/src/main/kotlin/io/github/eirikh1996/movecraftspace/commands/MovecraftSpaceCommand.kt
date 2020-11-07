package io.github.eirikh1996.movecraftspace.commands

import io.github.eirikh1996.movecraftspace.MovecraftSpace
import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.expansion.ExpansionState
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import org.bukkit.Bukkit.*
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

object MovecraftSpaceCommand : TabExecutor {
    override fun onCommand(sender : CommandSender, cmd : Command, label : String, args : Array<out String>): Boolean {
        if (!cmd.name.equals("movecraftspace", true))
            return false
        if (args.size == 0) {
            sender.sendMessage(COMMAND_PREFIX + "Version : " + MovecraftSpace.instance.description.version)
        } else if (args[0].equals("expansions", true)) {
            val set = HashSet<String>()
            for (ex in ExpansionManager.filter { e -> e.state != ExpansionState.NOT_LOADED }) {
                var entry = ""
                entry += if (ex.state == ExpansionState.ENABLED) {
                    "§a"
                } else {
                    "§c"
                }
                entry += ex.name
                entry += "§r"
                set.add(entry)
            }
            sender.sendMessage(COMMAND_PREFIX + "Expansions loaded: " + set.sortedBy { s: String -> ChatColor.stripColor(s) }.joinToString())
        }
        return true
    }

    override fun onTabComplete(sender : CommandSender, cmd : Command, label : String, args : Array<out String>) : List<String> {
        val tabCompletions = listOf("expansions").sorted()
        if (args.size == 0)
            return tabCompletions
        val completions = ArrayList<String>()
        for (c in tabCompletions) {
            if (!c.startsWith(args[args.size - 1]))
                continue
            completions.add(c)
        }
        completions.sort()
        return completions;
    }
}