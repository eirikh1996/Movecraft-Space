package io.github.eirikh1996.movecraftspace.commands

import io.github.eirikh1996.movecraftspace.MovecraftSpace
import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.Star
import io.github.eirikh1996.movecraftspace.objects.StarCollection
import io.github.eirikh1996.movecraftspace.objects.StarCollection.getStarByName
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import kotlin.math.min

object StarCommand : TabExecutor{
    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean {
        if (!cmd.name.equals("star", true))
            return false
        if (sender !is Player) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + "You must be player to use this command")
            return true
        }
        if (args.size == 0) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + "Usage /star <create|remove> <name>")
            return true
        }
        if (args[0].equals("create", true)) {
            if (args.size == 1) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "You need to supply a name")
                return true
            }
            val existingStar = getStarByName(args[1])
            if (existingStar != null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "A star named " + existingStar.name + " already exists")
                return true
            }
            val pLoc = sender.location.clone()
            pLoc.y = 127.0
            val closest = StarCollection.closestStar(pLoc, Settings.MinimumDistanceBetweenStars)
            if (closest != null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Star is too close to " + closest.name + ". Move " + (Settings.MinimumDistanceBetweenStars - closest.loc.distance(ImmutableVector.fromLocation(sender.location))).toInt() + " blocks away")
                return true
            }
            val newStar = Star(args[1], sender.world, ImmutableVector.fromLocation(pLoc))
            StarCollection.add(newStar)
            sender.sendMessage(COMMAND_PREFIX + "Successfully created new star named " + newStar.name)

            val sphere = LinkedList(MSUtils.createSphere(126, newStar.loc))
            object : BukkitRunnable() {
                override fun run() {
                    val size = min(sphere.size, 10000)
                    if (sphere.isEmpty())
                        cancel()
                    for (i in 1..size) {
                        sphere.pop().toLocation(newStar.space).block.type = Material.GLOWSTONE
                    }
                }
            }.runTaskTimer(MovecraftSpace.instance,0,1)

        } else if (args[0].equals("remove", true)) {
            if (args.size == 1) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Usage /star remove <name>")
                return true
            }
            val star = getStarByName(args[1])
            if (star == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Star " + args[1] + "does not exist!")
                return true
            }
            sender.sendMessage(COMMAND_PREFIX + "Successfully removed star named " + star.name)
            val sphere = LinkedList(MSUtils.createSphere(126, star.loc))
            object : BukkitRunnable() {
                override fun run() {
                    val size = min(sphere.size, 10000)
                    if (sphere.isEmpty())
                        cancel()
                    for (i in 1..size) {
                        sphere.pop().toLocation(star.space).block.type = Material.AIR
                    }
                }
            }.runTaskTimer(MovecraftSpace.instance,0,1)
        } else if (args[0].equals("tp", true)) {
            if (args.size == 1) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Usage /star tp <name>")
                return true
            }
            val star = getStarByName(args[1])
            if (star == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Star " + args[1] + "does not exist!")
                return true
            }
            val tpLoc = star.loc.toLocation(star.space)
            tpLoc.y = 260.0
            sender.teleport(tpLoc)
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): List<String> {
        var tabCompletions = Arrays.asList("create", "remove", "info", "tp")
        if (args[0].equals("create",true) || args[0].equals("info",true)) {
            tabCompletions = StarCollection.asStringList
        } else if (args.size > 1) {
            return emptyList()
        }
        val completions = ArrayList<String>()
        for (completion in tabCompletions) {
            if (!completion.startsWith(args[args.size - 1], true)) {
                continue
            }
            completions.add(completion)
        }
        return completions
    }

}