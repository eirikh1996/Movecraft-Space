package io.github.eirikh1996.movecraftspace.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor

object MovecraftSpaceCommand : TabExecutor {
    override fun onCommand(sender : CommandSender, cmd : Command, label : String, args : Array<out String>): Boolean {
        TODO("Not yet implemented")
    }

    override fun onTabComplete(sender : CommandSender, cmd : Command, label : String, args : Array<out String>) : List<String> {
        TODO("Not yet implemented")
    }
}