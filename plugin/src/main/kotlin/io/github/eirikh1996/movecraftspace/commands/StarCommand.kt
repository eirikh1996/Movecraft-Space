package io.github.eirikh1996.movecraftspace.commands

import io.github.eirikh1996.movecraftspace.MovecraftSpace
import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.event.planet.PlanetRemoveEvent
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.objects.Star
import io.github.eirikh1996.movecraftspace.objects.StarCollection
import io.github.eirikh1996.movecraftspace.objects.StarCollection.getStarByName
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_NO_PERMISSION
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import io.github.eirikh1996.movecraftspace.utils.MSUtils.WARNING
import io.github.eirikh1996.movecraftspace.utils.Paginator
import net.kyori.adventure.text.Component
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.lang.NumberFormatException
import java.util.*
import kotlin.collections.HashSet
import kotlin.math.min

object StarCommand : TabExecutor{
    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean {
        if (!cmd.name.equals("star", true))
            return false
        if (sender !is Player) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + "You must be player to use this command")
            return true
        }
        if (!sender.hasPermission("movecraftspace.command.star")) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + COMMAND_NO_PERMISSION)
        }
        if (args.size == 0) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + "Usage /star <create|remove> <name> [radius]")
            return true
        }
        if (args[0].equals("create", true)) {
            if (!sender.hasPermission("movecraftspace.command.star.create")) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + COMMAND_NO_PERMISSION)
            }
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
            val space = pLoc.world!!
            var height = space.maxHeight.toDouble()
            height -= space.minHeight.toDouble()
            height /= 2.0
            height += space.minHeight.toDouble()
            height += .5
            pLoc.y = height
            val closest = StarCollection.closestStar(pLoc, Settings.MinimumDistanceBetweenStars)
            if (closest != null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Star is too close to " + closest.name + ". Move " + (Settings.MinimumDistanceBetweenStars - closest.loc.distance(ImmutableVector.fromLocation(sender.location))).toInt() + " blocks away")
                return true
            }
            var radius = height.toInt() - 1
            if (args.size >= 3) {
                try {
                    radius = min(args[2].toInt(), height.toInt() - 1)
                } catch (e : NumberFormatException) {
                    sender.sendMessage(COMMAND_PREFIX + ERROR + args[2] + " is not a number")
                    return true
                }
            }
            val newStar = Star(args[1], sender.world, ImmutableVector.fromLocation(pLoc), radius)
            StarCollection.add(newStar)
            sender.sendMessage(COMMAND_PREFIX + "Successfully created new star named " + newStar.name)

            val sphere = LinkedList(MSUtils.createSphere(radius, newStar.loc))
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
            if (!sender.hasPermission("movecraftspace.command.star.remove")) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + COMMAND_NO_PERMISSION)
            }
            if (args.size == 1) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Usage /star remove <name>")
                return true
            }
            val star = getStarByName(args[1])
            if (star == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Star " + args[1] + "does not exist!")
                return true
            }
            val planetsOrbitingStar = PlanetCollection.getPlanetsWithOrbitPoint(star.loc)
            val sphere = LinkedList<ImmutableVector>()

            if (!planetsOrbitingStar.isEmpty()) {
                if (args.size < 3 || !args[2].equals("confirm", true)) {
                    val component = TextComponent(COMMAND_PREFIX + WARNING + "Star " + args[1] + " is part of a planetary system. Do you still want to remove? ")
                    val confirm = TextComponent("§2[Confirm]")
                    confirm.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/star remove " + star.name + " confirm")
                    component.addExtra(confirm)
                    sender.spigot().sendMessage(ChatMessageType.CHAT, component)
                    return true
                }
                for (planet in planetsOrbitingStar) {
                    val structureRadius = planet.radius - 30
                    sphere.addAll(MSUtils.createSphere(structureRadius, planet.center))
                    PlanetCollection.remove(planet)
                    if (!planet.moons.isEmpty()) {
                        for (moon in planet.moons) {
                            Bukkit.getPluginManager().callEvent(PlanetRemoveEvent(moon))
                            sphere.addAll(MSUtils.createSphere(moon.radius - 30, moon.center))
                            PlanetCollection.remove(moon)
                        }
                        sender.sendMessage(COMMAND_PREFIX + "Removed " + planet.moons.size + " moons attached to " + planet.name)
                        planet.moons.clear()
                    }
                }
            }
            StarCollection.remove(star)
            sender.sendMessage(COMMAND_PREFIX + "Successfully removed star named " + star.name)
            sphere.addAll(MSUtils.createSphere(star.radius, star.loc))
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
            if (!sender.hasPermission("movecraftspace.command.star.tp")) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + COMMAND_NO_PERMISSION)
            }
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
        } else if (args[0].equals("list", true)) {
            val paginator = Paginator("Stars")
            for (star in StarCollection) {
                val planets = PlanetCollection.getPlanetsWithOrbitPoint(star.loc)
                var moons = 0
                for (pl in planets) {
                    moons += pl.moons.size
                }
                paginator.addLine(Component.text(star.name + " Location: " + star.loc + " Space: " + star.space.name))
            }
            var pageNo = 1
            if (args.size > 1) {
                try {
                    pageNo = args[1].toInt()
                } catch (e : NumberFormatException) {
                    sender.sendMessage(COMMAND_PREFIX + ERROR + args[1] + " is not a number")
                }
            }
            paginator.getPage(pageNo, "/star list ").forEach { t ->
                if (t != null) {
                    sender.sendMessage(t)
                }
            }
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): List<String> {
        var tabCompletions = Arrays.asList("create", "remove", "info", "tp")
        if (!sender.hasPermission("movecraftspace.command.star")) {
            return emptyList()
        }
        if (args[0].equals("info",true) || args[0].equals("tp",true) ) {
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