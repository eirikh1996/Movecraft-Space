package io.github.eirikh1996.movecraftspace.commands

import io.github.eirikh1996.movecraftspace.MovecraftSpace
import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.listener.PlayerListener
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.Planet
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.objects.StarCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
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
import kotlin.collections.ArrayList
import kotlin.math.min

object PlanetCommand : TabExecutor {
    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean {
        if (!cmd.name.equals("planet", true)) {
            return false
        }

        if (sender !is Player) {
            sender.sendMessage("You must be player to use this command")
            return true
        }

        if (args.size == 0) {
            sender.sendMessage("Usage: /planet <create|remove|list> <radius> <destination> [exit height] [orbit time] [planet]")
            return true
        }
        if (args[0].equals("create", true)) {
            if (sender !is Player) {
                sender.sendMessage("You must be a player to use this command")
                return true
            }
            if (args.size == 1) {
                sender.sendMessage("You must specify destination world")
                return true
            } else if (args.size == 2) {
                sender.sendMessage("You must specify radius")
                return true
            }
            val radius = args[2].toInt()
            val destination = Bukkit.getWorld(args[1])
            if (destination == null) {
                sender.sendMessage("World " + args[1] + " does not exist!")
                return true
            } else if (sender.world.equals(destination)) {
                sender.sendMessage("Space world and planet world cannot be the same!")
                return true
            } else if (PlanetCollection.worldIsPlanet(sender.world)) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Planet worlds cannot serve as space worlds")
                return true
            }
            val nearestStar = StarCollection.closestStar(sender.location)
            if (nearestStar == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "No stars nearby. Create one using /star create <name>")
                return true
            }
            val exitHeight = if (args.size == 4) args[3].toInt() else 250
            val pLoc = sender.location.clone()
            val orbitTime = if (args.size == 5) {
                try {
                    args[4].toInt()
                } catch (e : NumberFormatException) {
                    sender.sendMessage(COMMAND_PREFIX + ERROR + "Orbit time is not a number")
                    return true
                }

            } else {
                10
            }
            val nearestPlanet = PlanetCollection.nearestPlanet(pLoc.world!!, ImmutableVector.fromLocation(pLoc), Settings.MaxMoonSpacing, true)

            val orbitCenter = if (nearestPlanet != null) {
                nearestPlanet.center
            } else {
                nearestStar.loc
            }
            pLoc.y = 127.5
            val planet = Planet(ImmutableVector.fromLocation(pLoc), orbitCenter, radius, pLoc.world!!, destination, orbitTime, exitHeight)
            if (PlanetCollection.getPlanetByName(args[1]) != null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Planet " + args[1] + " already exists!")
                return true
            }
            val spacing = planet.orbitRadius - nearestStar.radius()
            var moon = false
            if (nearestPlanet != null) {
                val moonSpacing = nearestPlanet.center.distance(planet.center)
                if (moonSpacing < Settings.MinMoonSpacing) {
                    sender.sendMessage(COMMAND_PREFIX + ERROR + "Planet is too close to the outer orbit of " + nearestPlanet.destination.name + "'s moons. Move " + (Settings.MinMoonSpacing - moonSpacing) + " blocks away")
                    return true
                } else if (moonSpacing > Settings.MaxMoonSpacing) {
                    sender.sendMessage(COMMAND_PREFIX + ERROR + "Planet is too far away from the outer orbit of " + nearestPlanet.destination.name + "'s moons. Move " + (Settings.MinMoonSpacing - moonSpacing) + " blocks away")
                    return true
                }
                moon = true
            }
            if (!moon && spacing < Settings.MinimumDistanceBetweenOrbits) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Planet is too close to the outer orbit of " + nearestStar.name + "'s planetary system. Move " + (Settings.MinimumDistanceBetweenOrbits - spacing) + " blocks away")
                return true
            } else if (!moon && spacing > Settings.MaximumDistanceBetweenOrbits) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Planet is too far away from the outer orbit of " + nearestStar.name + "'s planetary system. Move " + (spacing - Settings.MaximumDistanceBetweenOrbits ) + " blocks closer")
                return true
            }
            sender.sendMessage(COMMAND_PREFIX + "Successfully created planet " + args[1] + "!")
            val structureRadius = planet.radius - 30
            val sphere = LinkedList(MSUtils.createSphere(structureRadius, planet.center))
            object : BukkitRunnable() {
                override fun run() {
                    val size = min(sphere.size, 20000)
                    if (sphere.isEmpty())
                        cancel()
                    for (i in 0..size) {
                        sphere.pop().toLocation(planet.space).block.type = Material.STONE
                    }
                }
            }.runTaskTimer(MovecraftSpace.instance,0,1)

            if (nearestPlanet != null) {
                nearestPlanet.moons.add(planet)
            }
            PlanetCollection.add(planet)
        } else if (args[0].equals("remove", true)) {
            if (sender !is Player) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "You must be a player to use this command")
                return true
            }
            if (args.size == 1) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "You must specify a planet")
                return true
            }
            val planet = PlanetCollection.getPlanetByName(args[1])
            if (planet == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Planet " + args[1] + " does not exist!")
                return true
            }
            sender.sendMessage(COMMAND_PREFIX + "Successfully removed planet " + args[1] + "!")
            PlanetCollection.remove(planet)
            val structureRadius = planet.radius - 30
            val sphere = LinkedList(MSUtils.createSphere(structureRadius, planet.center))
            object : BukkitRunnable() {
                override fun run() {
                    val size = min(sphere.size, 20000)
                    if (sphere.isEmpty())
                        cancel()
                    for (i in 1..size) {
                        sphere.pop().toLocation(planet.space).block.type = Material.AIR
                    }
                }
            }.runTaskTimer(MovecraftSpace.instance,0,1)
        } else if (args[0].equals("toggleplayerteleport", true)) {
            if (PlayerListener.disabledPlayers.contains(sender.uniqueId)) {
                sender.sendMessage("Player teleportation to planets enabled")
                PlayerListener.disabledPlayers.remove(sender.uniqueId)
                return true
            }
            sender.sendMessage("Player teleportation to planets disabled")
            PlayerListener.disabledPlayers.add(sender.uniqueId)
        } else if (args[0].equals("move", true)) {
            if (sender !is Player) {
                sender.sendMessage("You must be a player to use this command")
                return true
            }
            if (args.size == 1) {
                sender.sendMessage("You must specify a planet")
                return true
            }
            val planet = PlanetCollection.getPlanetByName(args[1])
            if (planet == null) {
                sender.sendMessage("Planet " + args[1] + " does not exist!")
                return true
            }
            val tx = sender.location.blockX
            val tz = sender.location.blockZ
            val dx = tx - planet.center.x
            val dz = tz - planet.center.z
            sender.sendMessage("Moved planet to x: " + sender.location.blockX + ", z: " + sender.location.blockZ)
            val displacement = ImmutableVector(dx, 0, dz)
            planet.move(displacement)
            if (planet.moons.isEmpty())
                return true
            for (moon in planet.moons) {
                moon.move(displacement, true)
            }
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, label: String, args: Array<out String>) : List<String> {
        var tabCompletions = listOf("create", "remove", "move", "list", "toggleplayerteleport")
        tabCompletions = tabCompletions.sorted()
        if (args.size == 0) {
            return tabCompletions
        } else if (args[0].equals("create", true) && args.size >= 2 && args.size <= 3) {
            val worlds = ArrayList<String>()
            for (world in Bukkit.getWorlds()) {
                if (PlanetCollection.getPlanetByName(world.name) != null) {
                    continue
                }
                worlds.add(world.name)
            }
            tabCompletions = worlds
        } else if (args[0].equals("remove", true) || args[0].equals("move", true)) {
            val worlds = ArrayList<String>()
            for (planet in PlanetCollection) {
                worlds.add(planet.destination.name)
            }
            tabCompletions = worlds
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