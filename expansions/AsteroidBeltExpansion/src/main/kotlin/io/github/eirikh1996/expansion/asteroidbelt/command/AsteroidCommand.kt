package io.github.eirikh1996.expansion.asteroidbelt.command

import io.github.eirikh1996.expansion.asteroidbelt.AsteroidBeltExpansion
import io.github.eirikh1996.expansion.asteroidbelt.`object`.Asteroid
import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.selection.SelectionManager
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

object AsteroidCommand : TabExecutor {
    override fun onTabComplete(
        sender: CommandSender,
        cmd: Command,
        label: String,
        args: Array<out String>
    ): List<String>? {
        if (!cmd.name.equals("asteroid", true)) {
            return emptyList()
        }
        var tabCompletions = listOf("save", "paste")
        if (args.isEmpty())
            return tabCompletions
        else if (args[0].equals("paste", true))
            tabCompletions = AsteroidBeltExpansion.instance.asteroids.keys.toList()
        return tabCompletions.filter { s -> s.startsWith(args[args.size - 1]) }
    }

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean {
        if (!cmd.name.equals("asteroid", true))
            return false

        if (sender !is Player) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + MSUtils.MUST_BE_PLAYER)
            return true
        }
        if (!sender.hasPermission("movecraftspace.asteroidbelt.command.asteroid")) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + MSUtils.COMMAND_NO_PERMISSION)
            return true
        }
        if (args.isEmpty()) {
            return true
        }
        val selection = SelectionManager.selections[sender.uniqueId]
        if (selection == null) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + "You have no selection. Make one with your selection wand, which is a " + Settings.SelectionWand)
            return true
        }
        if (args[0].equals("save", true)) {
            if (args.size < 2) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "You need to supply a name")
                return true
            }
            val asteroid = Asteroid(args[1])
            asteroid.copy(selection)
            asteroid.save()
            AsteroidBeltExpansion.instance.asteroids[asteroid.name] = asteroid
        } else if (args[0].equals("paste", true)) {
            if (args.size < 2) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "You need to supply a name")
                return true
            }
            val asteroid = AsteroidBeltExpansion.instance.asteroids[args[1].lowercase()]
            if (asteroid == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Asteroid ${args[1].lowercase()} does not exist")
                return true
            }
            val target = sender.location.clone().add(asteroid.xLength.toDouble(), 0.0, 0.0)
            sender.sendMessage(COMMAND_PREFIX + "Pasted ${asteroid.name} at ${target.toVector()}")
            asteroid.paste(target)
        }
        return true
    }
}