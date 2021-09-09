package io.github.eirikh1996.expansion.asteroidbelt.command

import io.github.eirikh1996.expansion.asteroidbelt.AsteroidBeltExpansion
import io.github.eirikh1996.expansion.asteroidbelt.`object`.Asteroid
import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.selection.SelectionManager
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.objects.Star
import io.github.eirikh1996.movecraftspace.objects.StarCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_NO_PERMISSION
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import io.github.eirikh1996.movecraftspace.utils.MSUtils.MUST_BE_PLAYER
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
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.random.Random

object AsteroidBeltCommand : TabExecutor {
    val asteroidLocations = HashMap<String, Map<ImmutableVector, Asteroid>>()

    override fun onTabComplete(
        sender: CommandSender,
        cmd: Command,
        label: String,
        args: Array<out String>
    ): List<String>? {
        val completions = ArrayList<String>()
        if (!sender.hasPermission("movecraftspace.asteroidbelt.command.asteroid")) {
            return completions
        }
        if (args.size <= 1) {
            completions.add("create")
            completions.add("regenerate")
            completions.add("remove")
        } else if (args[0].equals("regenerate", true) || args[0].equals("remove", true)) {
            completions.addAll(asteroidLocations.keys)
        }
        if (args.isEmpty())
            return completions
        return completions.filter { s -> s.startsWith(args[args.size - 1]) }
    }

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean {
        if (!cmd.name.equals("asteroidbelt", true))
            return false
        if (sender !is Player) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + MUST_BE_PLAYER)
            return true
        }
        if (!sender.hasPermission("movecraftspace.asteroidbelt.command.asteroid")) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + COMMAND_NO_PERMISSION)
            return true
        }
        if (args.isEmpty()) {
            return true
        } else if (args[0].equals("create", true)) {
            if (args.size < 5) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Correct syntax is /asteroidbelt create <star> <minRadius> <maxRadius> <height>")
                return true
            }
            val star = StarCollection.getStarByName(args[1])

            if (star == null){
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Star ${args[1]} does not exist")
                return true
            }
            val minRadius : Int
            val maxRadius : Int
            val height : Int
            try {
                minRadius = args[2].toInt()
            } catch (e : NumberFormatException) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + args[2] + " is not a number")
                return true
            }
            try {
                maxRadius = args[3].toInt()
            } catch (e : NumberFormatException) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + args[3] + " is not a number")
                return true
            }
            try {
                height = args[4].toInt()
            } catch (e : NumberFormatException) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + args[4] + " is not a number")
                return true
            }
            for (pl in PlanetCollection) {
                val pMinR = pl.orbitRadius - pl.radius
                val pMaxR = pl.orbitRadius + pl.radius
                if (minRadius in pMinR..pMaxR) {
                    sender.sendMessage(COMMAND_PREFIX + ERROR + "Minimum radius $minRadius intersects with the planetary orbit of " + pl.name)
                    return true
                }
                if (maxRadius in pMinR..pMaxR) {
                    sender.sendMessage(COMMAND_PREFIX + ERROR + "Maximum radius $maxRadius intersects with the planetary orbit of " + pl.name)
                    return true
                }
            }

            val locations = HashSet<Location>()
            val asteroids = HashMap<ImmutableVector, Asteroid>()

            var clearLocationsLeft = true
            val asteroidbeltTag = "${star.name}-$minRadius-$maxRadius".lowercase()
            while (clearLocationsLeft) {
                clearLocationsLeft = false
                for (asteroid in AsteroidBeltExpansion.instance.asteroids.values) {
                    val randomLoc = randomLoc(star.loc, minRadius, maxRadius, height, asteroid.xLength / 2, asteroid.yLength / 2, asteroid.zLength / 2)
                    val blockLocs = HashSet<Location>()
                    asteroid.blocks.forEach { (t, u) ->
                        blockLocs.add(randomLoc.add(t).toLocation(star.space))
                    }
                    if (blockLocs.any { loc -> locations.contains(loc) || locations.any { o -> o.distance(loc) < AsteroidBeltExpansion.instance.distanceBetweenAsteroids } }) {
                        continue
                    }
                    locations.addAll(blockLocs)
                    asteroids[randomLoc] = asteroid
                    clearLocationsLeft = true
                    break
                }
                asteroidLocations[asteroidbeltTag] = asteroids
            }
            sender.sendMessage(COMMAND_PREFIX + "Started creating asteroid belt")
            object : BukkitRunnable() {
                val linkedList = LinkedList(asteroids.keys)
                val initialSize = linkedList.size

                override fun run() {
                    if (linkedList.isEmpty()) {
                        sender.sendMessage(COMMAND_PREFIX + "Complete")
                        cancel()
                    }
                    val poll = linkedList.poll()
                    val percent = ((initialSize - linkedList.size) / initialSize) * 100f
                    asteroids[poll]?.paste(poll.toLocation(star.space))
                    if (percent % 10.0 == 0.0)
                        sender.sendMessage(COMMAND_PREFIX + "Current progress: $percent %")

                }

            }.runTaskTimer(AsteroidBeltExpansion.instance.plugin, 0, 10)

        } else if (args[0].equals("regenerate", true)) {
            if (args.size < 2) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "You need to supply an asteroid belt")
                return true
            }
            val locs = asteroidLocations[args[1].lowercase()]
            if (locs == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Asteroid belt ${args[1].lowercase()} does not exist")
                return true
            }
            val star = StarCollection.getStarByName(args[1].lowercase().split("-")[0])
            if (star == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Star ${args[1].lowercase().split("-")[0]} does not exist")
                return true
            }
            locs.forEach { (t, u) ->
                u.paste(t.toLocation(star.space))
            }
        } else if (args[0].equals("remove", true)) {
            if (args.size < 2) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "You need to supply an asteroid belt")
                return true
            }
            val locs = asteroidLocations[args[1].lowercase()]
            if (locs == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Asteroid belt ${args[1].lowercase()} does not exist")
                return true
            }
            val star = StarCollection.getStarByName(args[1].lowercase().split("-")[0])
            if (star == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Star ${args[1].lowercase().split("-")[0]} does not exist")
                return true
            }
            locs.forEach { (t, u) ->
                u.blocks.forEach { (l, b) ->
                    l.add(t).toLocation(star.space).block.type = Material.AIR
                }
            }
        }
        TODO()
    }

    private fun randomLoc(center : ImmutableVector, minRadius : Int, maxRadius : Int, height : Int, xbuffer : Int = 0, ybuffer : Int = 0, zbuffer : Int = 0) : ImmutableVector {
        var loc = ImmutableVector(
            Random.nextInt(center.x - (maxRadius - xbuffer), center.x + (maxRadius - xbuffer)),
            Random.nextInt(center.y - ((height / 2) - ybuffer), center.y + ((height / 2) - ybuffer)),
            Random.nextInt(center.z - (maxRadius - zbuffer), center.z + (maxRadius - zbuffer))
        )
        while ((loc.distance(center).toInt() in minRadius..maxRadius)) {
            loc = ImmutableVector(
                Random.nextInt(center.x - (maxRadius - xbuffer), center.x + (maxRadius - xbuffer)),
                Random.nextInt(center.y - ((height / 2) - ybuffer), center.y + ((height / 2) - ybuffer)),
                Random.nextInt(center.z - (maxRadius - zbuffer), center.z + (maxRadius - zbuffer))
            )
        }
        return loc
    }
}