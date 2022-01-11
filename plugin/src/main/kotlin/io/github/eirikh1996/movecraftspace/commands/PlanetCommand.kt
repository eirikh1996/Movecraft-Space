package io.github.eirikh1996.movecraftspace.commands

import io.github.eirikh1996.movecraftspace.MovecraftSpace
import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.event.planet.PlanetCreateEvent
import io.github.eirikh1996.movecraftspace.event.planet.PlanetRemoveEvent
import io.github.eirikh1996.movecraftspace.listener.PlayerListener
import io.github.eirikh1996.movecraftspace.objects.*
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_NO_PERMISSION
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import io.github.eirikh1996.movecraftspace.utils.MSUtils.WARNING
import io.github.eirikh1996.movecraftspace.utils.MSUtils.setBlock
import io.github.eirikh1996.movecraftspace.utils.Paginator
import io.github.eirikh1996.movecraftspace.utils.image.RGBBlockColor
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import org.bukkit.Bukkit
import org.bukkit.Bukkit.getPluginManager
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.lang.NumberFormatException
import java.lang.System.currentTimeMillis
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.abs
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

        if (args.isEmpty()) {
            sender.sendMessage("Usage: /planet <create|remove|list> <radius> <destination> [exitheight:numValue orbittime:numValue] ")
            return true
        }
        if (args[0].equals("create", true)) {
            if (!sender.hasPermission("movecraftspace.command.planet.create")) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + COMMAND_NO_PERMISSION)
                return true
            }
            if (sender !is Player) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "You must be a player to use this command")
                return true
            }
            if (args.size == 1) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "You must specify destination world")
                return true
            } else if (args.size == 2) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "You must specify radius")
                return true
            }
            val radius : Int
            try {
                radius = args[2].toInt()
            } catch (e : NumberFormatException) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + args[2] + " is not a number!")
                return true
            }
            val destination = Bukkit.getWorld(args[1])
            if (destination == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "World " + args[1] + " does not exist!")
                return true
            } else if (sender.world == destination) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Space world and planet world cannot be the same!")
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
            var exitHeight =  destination.maxHeight - 5
            var orbitTime = 10
            var surface : Material? = Material.STONE
            var data = 0.toByte()
            var isMoon = ""
            val planetProperties = ArrayList<String>()
            for (arg in args.drop(2)) {
                try {
                    if (arg.startsWith("exitheight:", true)) {
                        planetProperties.add(arg)
                        exitHeight = arg.replace("exitheight:", "", true).toInt()
                    }
                    if (arg.startsWith("orbittime:", true)) {
                        planetProperties.add(arg)
                        orbitTime = arg.replace("orbittime:", "", true).toInt()
                    }
                    if (arg.startsWith("surfacematerial:", true)) {
                        planetProperties.add(arg)
                        val str = arg.replace("surfacematerial:", "", true)
                        if (str.contains(":")) {
                            val pts = str.split(":")
                            surface = Material.getMaterial(pts[0].uppercase())
                            data = pts[1].toByte()
                        } else {
                            surface = Material.getMaterial(str.uppercase())
                        }
                    }
                    if (arg.startsWith("isMoon:", true)) {
                        isMoon = arg.replace("isMoon:", "", true)
                        if (!isMoon.equals("true", true) && !isMoon.equals("false", true)) {
                            return true
                        }
                    }
                } catch (e : NumberFormatException) {
                    sender.sendMessage(COMMAND_PREFIX + ERROR + "Invalid argument: " + arg)
                    return true
                }
                if (surface == null) {
                    sender.sendMessage(COMMAND_PREFIX + ERROR + "Invalid material: " + arg)
                    return true
                }
            }


            val pLoc = sender.location.clone()
            var nearestPlanet : Planet? = null
            if (isMoon.isEmpty() || isMoon.toBoolean())
                nearestPlanet = PlanetCollection.nearestPlanet(pLoc.world!!, ImmutableVector.fromLocation(pLoc), Settings.MaxMoonSpacing, true)

            val orbitCenter = if (nearestPlanet != null) {
                nearestPlanet.center
            } else {
                nearestStar.loc
            }
            val space = pLoc.world!!
            var height = space.maxHeight.toDouble()
            if (Settings.IsV1_17) {
                height -= space.minHeight.toDouble()
            }
            height /= 2.0
            if (Settings.IsV1_17)
                height += space.minHeight.toDouble()
            height += .5
            pLoc.y = height
            val planet = Planet(ImmutableVector.fromLocation(pLoc), orbitCenter, radius, space, destination, orbitTime, exitHeight)
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
            if (moon && isMoon == "") {
                val text = TextComponent(COMMAND_PREFIX + "Planet " + planet.name + " is within the moon radius of " + nearestPlanet!!.name + " and will become a moon of this planet. Should planet be moon? ")
                val yesClickText = TextComponent("§2[Yes] ")
                yesClickText.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/planet create " + planet.name + " " + planet.radius + " " + planetProperties.joinToString(" ") + "isMoon:true" )
                yesClickText.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, arrayOf(TextComponent("Click to set planet " + planet.name + " as moon of " + nearestPlanet!!.name)))
                val noClickText = TextComponent("§4[No] ")
                noClickText.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/planet create " + planet.name + " " + planet.radius + " " + planetProperties.joinToString(" ") + "isMoon:false" )
                noClickText.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, arrayOf(TextComponent("Click to set planet " + planet.name + " as a planet orbiting the star " + nearestStar.name)))
                text.addExtra(yesClickText)
                text.addExtra(noClickText)
                sender.spigot().sendMessage(text)
                return true
            }
            val event = PlanetCreateEvent(planet)
            getPluginManager().callEvent(event)
            if (event.isCancelled)
                return true
            sender.sendMessage(COMMAND_PREFIX + "Successfully created planet " + planet.name + "!")
            val structureRadius = planet.radius - 30
            val sphere = LinkedList(MSUtils.createSphere(structureRadius, planet.center))

            object : BukkitRunnable() {

                override fun run() {
                    val size = min(sphere.size, 20000)
                    if (sphere.isEmpty())
                        cancel()
                    for (i in 0..size) {
                        val pop = sphere.pop()
                        val loc = pop.toLocation(planet.space)
                        val b = loc.block
                        b.type = surface!!
                        if (Settings.IsLegacy)
                            Block::class.java.getDeclaredMethod("setData", Byte::class.java).invoke(b, data)
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
                planetRemoveTimeMap.containsKey(sender.uniqueId) &&
                currentTimeMillis() - planetRemoveTimeMap.get(sender.uniqueId)!! > 10000) {
                sender.sendMessage(COMMAND_PREFIX + WARNING + "Planet " + planet.name + " is center of a moon system. Removal will also remove its moons. Type /planet remove " + planet.name + " again within 10 seconds to remove.")
                planetRemoveTimeMap.put(sender.uniqueId, currentTimeMillis())
                return true
            }
            sender.sendMessage(COMMAND_PREFIX + "Successfully removed planet " + args[1] + "!")
            PlanetCollection.remove(planet)
            val structureRadius = planet.radius - 30
            val sphere = LinkedList(MSUtils.createSphere(structureRadius, planet.center))
            getPluginManager().callEvent(PlanetRemoveEvent(planet))
            if (!planet.moons.isEmpty()) {
                for (moon in planet.moons) {
                    getPluginManager().callEvent(PlanetRemoveEvent(moon))
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
            if (!sender.hasPermission("movecraftspace.command.planet.move")) {
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
            val tx = sender.location.blockX
            val tz = sender.location.blockZ
            val closestStar = StarCollection.closestStarSystem(
                Location(sender.world, tx.toDouble(), 127.0,
                    tz.toDouble()
                ), Settings.MaximumDistanceBetweenOrbits
            )
            if (closestStar == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Cannot move planet to this location as it it too far away from a star or planetary system")
                return true
            }
            val closestPlanet = PlanetCollection.nearestPlanet(sender.world, ImmutableVector.fromLocation(sender.location), Settings.MaxMoonSpacing, true)
            val distance = closestStar.loc.distance(ImmutableVector(tx, 127, tz)) - closestStar.radius()
            if (distance < Settings.MinimumDistanceBetweenOrbits && closestPlanet == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Cannot move planet to this location as it it too close to star system " + closestStar.name)
                return true
            } else if (closestPlanet != null) {
                if (closestPlanet.center.distance(ImmutableVector.fromLocation(sender.location)) < Settings.MinMoonSpacing) {
                    sender.sendMessage(COMMAND_PREFIX + ERROR + "Cannot move planet to this location as it it too close to moon system " + closestPlanet.name)
                    return true
                }
                if (!closestPlanet.moons.isEmpty()) {
                    sender.sendMessage(COMMAND_PREFIX + ERROR + "Planets with moons cannot join other moon systems")
                    return true
                }

            }
            val dx = tx - planet.center.x
            val dz = tz - planet.center.z
            sender.sendMessage(COMMAND_PREFIX + "Moved planet to x: " + sender.location.blockX + ", z: " + sender.location.blockZ)
            val displacement = ImmutableVector(dx, 0, dz)
            planet.move(displacement, false, sender.world)
            planet.orbitCenter = if (closestPlanet == null) closestStar.loc else closestPlanet.center
            for (pl in PlanetCollection) {
                pl.moons.remove(planet)
            }
            if (closestPlanet != null)
                closestPlanet.moons.add(planet)

            if (planet.moons.isEmpty())
                return true
            for (moon in planet.moons) {
                moon.move(displacement, true, sender.world)
            }
        } else if (args[0].equals("list", true)) {
            if (!sender.hasPermission("movecraftspace.command.planet.list")) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + COMMAND_NO_PERMISSION)
                return true
            }
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
                val component = TextComponent(pl.name + ": §cLocation:§r " + pl.center + ", §cSystem:§r " + systemName + ", §cOrbit time:§r " + pl.orbitTime + "§cSpace world:§r " + pl.space.name)
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
            if (!sender.hasPermission("movecraftspace.command.planet.regensphere")) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + COMMAND_NO_PERMISSION)
                return true
            }
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
            var surface : Material? = Material.STONE
            var data = 0.toByte()
            if (args.size >= 3) {
                try {
                    if (args[2].contains(":")) {
                        val pts = args[2].split(":")
                        surface = Material.getMaterial(pts[0].toUpperCase())
                        data = pts[1].toByte()
                    } else {
                        surface = Material.getMaterial(args[2].toUpperCase())
                    }
                } catch (e: NumberFormatException) {
                    sender.sendMessage(COMMAND_PREFIX + ERROR + "Invalid argument: " + args[2])
                    return true
                }
            }
            if (surface == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Invalid argument: " + args[2])
                return true
            }
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
                            val loc = pop.toLocation(planet.space)
                            setBlock(loc, surface, if (Settings.IsLegacy) data else Bukkit.createBlockData(surface))
                        }
                    } else {
                        cancel()
                    }
                }

            }.runTaskTimer(MovecraftSpace.instance, 0, 3)
        } else if (args[0].equals("tp", true)) {
            if (!sender.hasPermission("movecraftspace.command.planet.tp")) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + COMMAND_NO_PERMISSION)
                return true
            }
            if (args.size <= 1) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "You must specify a planet")
                return true
            }
            val planet = PlanetCollection.getPlanetByName(args[1])
            if (planet == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Planet " + args[1] + " does not exist!")
                return true
            }
            sender.teleport(Location(planet.space,
                planet.center.x.toDouble(),
                (planet.center.y + planet.radius + 10).toDouble(),
                planet.center.z.toDouble(),
                sender.location.yaw,
                sender.location.pitch))
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, label: String, args: Array<out String>) : List<String> {
        Bukkit.broadcastMessage(args.size.toString())
        var tabCompletions = listOf("create", "remove", "move", "list", "toggleplayerteleport", "regensphere", "tp").filter { str -> sender.hasPermission("movecraftspace.command.planet." + str) }
        tabCompletions = tabCompletions.sorted()
        if (args.isEmpty()) {
            return tabCompletions
        } else if (args[0].equals("create", true) && args.size >= 2 && args.size <= 4) {
            val worlds = ArrayList<String>()
            for (world in Bukkit.getWorlds()) {
                if (PlanetCollection.getPlanetByName(world.name) != null) {
                    continue
                }
                worlds.add(world.name)
            }
            tabCompletions = worlds
            if (args.size == 3) {
                tabCompletions = emptyList()
            }
            if (args.size >= 4) {
                tabCompletions = listOf("exitheight:", "surfacematerial:", "isMoon:", "orbitTime:")
                for (arg in args.drop(3)) {
                    if (arg.startsWith("exitheight:", true) || arg.startsWith("orbitTime:", true)) {
                        tabCompletions = emptyList()
                    }
                    if (arg.startsWith("surfacematerial:", true)) {
                        tabCompletions = ArrayList()
                        Material.values().forEach { type -> (tabCompletions as ArrayList<String>).add(type.name.lowercase()) }
                    }
                    if (arg.startsWith("isMoon:", true)) {
                        tabCompletions = listOf("true", "false")
                    }
                }
            }
        } else if (args[0].equals("remove", true) ||
            args[0].equals("move", true) ||
            args[0].equals("regensphere", true)||
            args[0].equals("tp", true)) {
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