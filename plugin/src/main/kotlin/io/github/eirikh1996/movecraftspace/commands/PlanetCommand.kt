package io.github.eirikh1996.movecraftspace.commands

import io.github.eirikh1996.movecraftspace.MovecraftSpace
import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.listener.PlayerListener
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.Planet
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.objects.StarCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_NO_PERMISSION
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import io.github.eirikh1996.movecraftspace.utils.MSUtils.WARNING
import io.github.eirikh1996.movecraftspace.utils.Paginator
import net.md_5.bungee.api.ChatMessageType
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
import java.lang.System.currentTimeMillis
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.min
import kotlin.random.Random

object PlanetCommand : TabExecutor {
    val planetRemoveTimeMap = HashMap<UUID, Long>()

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean {
        if (!cmd.name.equals("planet", true)) {
            return false
        }
        if (!sender.hasPermission("movecraftspace.command.planet")) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + COMMAND_NO_PERMISSION)
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("You must be player to use this command")
            return true
        }

        if (args.size == 0) {
            sender.sendMessage("Usage: /planet <create|remove|list> <radius> <destination> [exitheight:numValue orbit time:numValue] ")
            return true
        }
        if (args[0].equals("create", true)) {
            if (!sender.hasPermission("movecraftspace.command.planet.create")) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + COMMAND_NO_PERMISSION)
                return true
            }
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
            var exitHeight =  250
            var orbitTime = 10
            for (arg in args.drop(2)) {
                try {
                    if (arg.startsWith("exitheight:", true)) {
                        exitHeight = arg.replace("exitheight:", "", true).toInt()
                    }
                    if (arg.startsWith("orbittime:", true)) {
                        orbitTime = arg.replace("orbittime:", "", true).toInt()
                    }
                } catch (e : NumberFormatException) {
                    sender.sendMessage("Invalid argument: " + arg)
                }
            }


            val pLoc = sender.location.clone()
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
            if (nearestPlanet != null && nearestPlanet.center.distance(planet.center) < nearestStar.loc.distance(planet.center)) {
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
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Planet is too close to the outer orbit around " + nearestStar.name + "'s planetary system. Move " + (Settings.MinimumDistanceBetweenOrbits - spacing) + " blocks away")
                return true
            } else if (!moon && spacing > Settings.MaximumDistanceBetweenOrbits) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Planet is too far away from the outer orbit of " + nearestStar.name + "'s planetary system. Move " + (spacing - Settings.MaximumDistanceBetweenOrbits ) + " blocks closer")
                return true
            }
            val intersecting = PlanetCollection.intersectingOtherPlanetaryOrbit(planet, nearestPlanet)
            if (intersecting != null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Orbit of planet " + planet.name + " intersects with the orbit of " + intersecting.name)
                return true
            }
            sender.sendMessage(COMMAND_PREFIX + "Successfully created planet " + planet.name + "!")
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

            if (moon) {
                nearestPlanet!!.moons.add(planet)
            }
            PlanetCollection.add(planet)
        } else if (args[0].equals("remove", true)) {
            if (!sender.hasPermission("movecraftspace.command.planet.remove")) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + COMMAND_NO_PERMISSION)
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
            if (planet.moons.size > 0 &&
                !planetRemoveTimeMap.containsKey(sender.uniqueId) ||
                currentTimeMillis() - planetRemoveTimeMap.get(sender.uniqueId)!! > 10000) {
                sender.sendMessage(COMMAND_PREFIX + WARNING + "Planet " + planet.name + " is center of a moon system. Removal will also remove its moons. Type /planet remove " + planet.name + " again within 10 seconds to remove.")
                planetRemoveTimeMap.put(sender.uniqueId, currentTimeMillis())
                return true
            }
            sender.sendMessage(COMMAND_PREFIX + "Successfully removed planet " + args[1] + "!")
            PlanetCollection.remove(planet)
            val structureRadius = planet.radius - 30
            val sphere = LinkedList(MSUtils.createSphere(structureRadius, planet.center))
            if (!planet.moons.isEmpty()) {
                for (moon in planet.moons) {
                    sphere.addAll(MSUtils.createSphere(moon.radius - 30, moon.center))
                    PlanetCollection.remove(moon)
                }
                sender.sendMessage(COMMAND_PREFIX + "Removed " + planet.moons.size + " moons attached to " + planet.name)
                planet.moons.clear()
            }
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
            if (!sender.hasPermission("movecraftspace.command.planet.toggleplayerteleport")) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + COMMAND_NO_PERMISSION)
                return true
            }
            if (!Settings.AllowPlayersTeleportationToPlanets) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Player teleportation to planets disabled in config")
                return true
            }
            if (PlayerListener.disabledPlayers.contains(sender.uniqueId)) {
                sender.sendMessage(COMMAND_PREFIX + "Player teleportation to planets enabled")
                PlayerListener.disabledPlayers.remove(sender.uniqueId)
                return true
            }
            sender.sendMessage(COMMAND_PREFIX + "Player teleportation to planets disabled")
            PlayerListener.disabledPlayers.add(sender.uniqueId)
        } else if (args[0].equals("move", true)) {
            if (args.size == 1) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "You must specify a planet")
                return true
            }
            val planet = PlanetCollection.getPlanetByName(args[1])
            if (planet == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Planet " + args[1] + " does not exist!")
                return true
            }
            val tx = sender.location.blockX
            val tz = sender.location.blockZ
            val dx = tx - planet.center.x
            val dz = tz - planet.center.z
            sender.sendMessage(COMMAND_PREFIX + "Moved planet to x: " + sender.location.blockX + ", z: " + sender.location.blockZ)
            val displacement = ImmutableVector(dx, 0, dz)
            planet.move(displacement)
            if (planet.moons.isEmpty())
                return true
            for (moon in planet.moons) {
                moon.move(displacement, true)
            }
        } else if (args[0].equals("list", true)) {
            val paginator = Paginator("Planets")
            for (pl in PlanetCollection) {
                val orbitCenterPlanet = PlanetCollection.getPlanetAt(pl.orbitCenter.toLocation(pl.space))
                val orbitCenterStar = StarCollection.closestStar(pl.orbitCenter.toLocation(pl.space),2)
                val systemName = if (orbitCenterPlanet != null) {
                    orbitCenterPlanet.name
                } else if (orbitCenterStar != null) {
                    orbitCenterStar.name
                } else {
                    ""
                }
                val component = TextComponent(pl.name + ", §cSystem:§r " + systemName + ", §cOrbit time:§r " + pl.orbitTime)
                paginator.addLine(component)
            }
            val pageNo = if (args.size > 1) {
                try {
                    args[1].toInt()
                } catch (e : NumberFormatException) {
                    sender.sendMessage(COMMAND_PREFIX + ERROR + args[1] + " is not a number")
                    return true
                }
            } else {
                1
            }
            val page = paginator.getPage(pageNo, "/planet list")
            for (l in page) {
                if (l == null) {
                    continue
                }
                sender.spigot().sendMessage(ChatMessageType.CHAT, l)
            }
        } else if (args[0].equals("regensphere", true)) {
            if (args.size <= 1) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "You must specify a planet")
                return true
            }
            val planet = PlanetCollection.getPlanetByName(args[1])
            if (planet == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Planet " + args[1] + " does not exist!")
                return true
            }
            val newSphere = LinkedList(MSUtils.createSphere(planet.radius - 30, planet.center))
            object : BukkitRunnable() {
                var x = planet.minX
                var y = planet.minY
                var z = planet.minZ
                val cx = planet.center.x
                val cy = planet.center.y
                val cz = planet.center.z
                var clearedOldSphere = false
                override fun run() {
                    if (!clearedOldSphere) {
                        for (i in 0..30000) {
                            x++
                            if (x > planet.maxX) {
                                x = planet.minX
                                z++
                                if (z > planet.maxz) {
                                    z = planet.minZ
                                    y++
                                }
                            }
                            if (x >= planet.maxX && y >= planet.maxY && z >= planet.maxz) {
                                clearedOldSphere = true
                                break
                            }
                            val diffx = x - cx
                            val diffy = y - cy
                            val diffz = z - cz
                            val distSquared  = diffx * diffx + diffy * diffy + diffz * diffz
                            val radiusSquared = planet.radius * planet.radius
                            if (distSquared > radiusSquared)
                                continue
                            planet.space.getBlockAt(x,y,z).type = Material.AIR

                        }
                    } else if (!newSphere.isEmpty()) {
                        for (i in 1..min(newSphere.size, 25000)) {
                            val pop = newSphere.pop()
                            pop.toLocation(planet.space).block.type = Material.STONE
                        }
                    } else {
                        cancel()
                    }
                }

            }.runTaskTimer(MovecraftSpace.instance, 0, 3)
        } else if (args[0].equals("addclouds", true)) {
            if (args.size <= 1) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "You must specify a planet")
                return true
            }
            val planet = PlanetCollection.getPlanetByName(args[1])
            if (planet == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Planet " + args[1] + " does not exist!")
                return true
            }
            val percentage : Double
            if (args.size <= 2) {
                try {
                    percentage = args[1].toDouble()
                } catch (e : NumberFormatException) {
                    sender.sendMessage(COMMAND_PREFIX + ERROR + "Invalid argument " + args[1])
                    return true
                }
            } else {
                percentage = 100.0
            }
            val oldSphere = LinkedList(MSUtils.createSphere(planet.radius - 26, planet.center))
            val sphere = LinkedList(oldSphere.filter { loc -> Random.nextDouble(0.0, 100.0) <= percentage })
            object : BukkitRunnable() {
                override fun run() {
                    if (!oldSphere.isEmpty()) {
                        val size = min(oldSphere.size, 20000)
                        for (i in 1..size) {
                            oldSphere.pop().toLocation(planet.space).block.type = Material.AIR
                        }
                    } else if (!sphere.isEmpty()) {
                        val size = min(sphere.size, 20000)
                        for (i in 1..size) {
                            sphere.pop().toLocation(planet.space).block.type =
                                if (Settings.IsLegacy) Material.getMaterial("STAINED_GLASS")!! else Material.WHITE_STAINED_GLASS
                        }
                    } else
                        cancel()
                }
            }.runTaskTimer(MovecraftSpace.instance,0,1)
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, label: String, args: Array<out String>) : List<String> {
        var tabCompletions = listOf("create", "remove", "move", "list", "toggleplayerteleport", "regensphere", "addclouds").filter { str -> sender.hasPermission("movecraftspace.command.planet." + str) }
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
        } else if (args[0].equals("remove", true) ||
            args[0].equals("move", true) ||
            args[0].equals("regensphere", true) ||
            args[0].equals("addclouds", true)) {
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