package io.github.eirikh1996.movecraftspace.expansion.hyperspace.command

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.HyperspaceBeacon
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.HyperspaceExpansion
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.HyperspaceManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.HyperspaceTravelEntry
import io.github.eirikh1996.movecraftspace.expansion.selection.SelectionManager
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.MSBlock
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.objects.StarCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_NO_PERMISSION
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import io.github.eirikh1996.movecraftspace.utils.MSUtils.hitboxObstructed
import net.countercraft.movecraft.MovecraftLocation
import net.countercraft.movecraft.craft.CraftManager
import org.bukkit.Bukkit
import org.bukkit.Bukkit.getScheduler
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.boss.BarColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryHolder
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.lang.System.currentTimeMillis
import java.math.RoundingMode
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

object HyperspaceCommand : TabExecutor {
    val locationMap = HashMap<UUID, Location>()
    val beaconStructure = HashMap<ImmutableVector, MSBlock>()

    init {
        val file = File(HyperspaceExpansion.instance.dataFolder, "beaconstructure.yml")
        if (file.exists()) {
            val yml = YamlConfiguration.loadConfiguration(file)
            val mapList = yml.getMapList("blocks") as List<Map<String, Any>>
            for (map in mapList) {
                beaconStructure[ImmutableVector.deserialize(map)] = MSBlock.deserialize(map)
            }
        }
    }
    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        var tabCompletions = listOf("travel", "beacon", "pullout")
        if (args.isEmpty())
            return tabCompletions
        if (args[0].equals("beacon", true)) {
            tabCompletions = listOf("remove", "create", "save", "paste")
            if (args.size == 1) {
                return tabCompletions
            } else if (args[1].equals("remove", true)) {
                val links = ArrayList<String>()
                HyperspaceManager.beaconLocations.forEach { b -> links.add(b.originName + "-" + b.destinationName) }
                tabCompletions = links
            }

        } else if (args[0].equals("pullout", true)) {
            val players = ArrayList<String>()
            CraftManager.getInstance().getCraftsInWorld(HyperspaceExpansion.instance.hyperspaceWorld).forEach { craft -> players.add(craft.notificationPlayer?.name!!) }
            tabCompletions = players
        }
        return tabCompletions.filter { s -> s.startsWith(args[args.size - 1]) }.sorted()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!command.name.equals("HyperSpace", true)) {
            return false
        }
        if (sender !is Player) {
            sender.sendMessage(COMMAND_PREFIX + ERROR + "You must be player to use this command")
            return true
        }
        if (args.isEmpty()) {
            return true
        }
        if (args[0].equals("travel", true)) {
            val craft = CraftManager.getInstance().getCraftByPlayer(sender)
            if (craft == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "You are not commanding a space craft")
                return true
            }
            val hitBox = craft.hitBox
            var foundLoc : Location? = null
            var destination : Location? = null
            var str = ""
            for (beacon in HyperspaceManager.beaconLocations) {
                if (beacon.origin.world!! == craft.w && beacon.origin.distance(hitBox.midPoint.toBukkit(craft.w)) <= HyperspaceExpansion.instance.config.getInt("Beacon range")) {
                    foundLoc = beacon.origin
                    destination = beacon.destination
                    str = beacon.originName + "-" + beacon.destinationName
                    break
                }
                if (beacon.destination.world!!.equals(craft.w) && beacon.destination.distance(hitBox.midPoint.toBukkit(craft.w)) <= HyperspaceExpansion.instance.config.getInt("Beacon range")) {
                    foundLoc = beacon.destination
                    destination = beacon.origin
                    str = beacon.destinationName + "-" + beacon.originName
                    break
                }
            }
            if (foundLoc == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "You are not within the range of a hyperspace beacon")
                return true
            }
            HyperspaceManager.scheduleHyperspaceTravel(craft, foundLoc, destination!!, str, true)

        } else if (args[0].equals("beacon", true)) {
            if (!sender.hasPermission("movecraftspace.command.hyperspace.beacon")) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + COMMAND_NO_PERMISSION)
                return true
            }
            if (args.size < 2) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Correct syntax is /hyperspace beacon <create|remove|save|paste>")
                return true
            }
            if (args[1].equals("create", true)) {
                if (!PlanetCollection.any { p -> p.space.equals(sender.world) } && !StarCollection.any { s -> s.space.equals(sender.world) }) {
                    sender.sendMessage(COMMAND_PREFIX + ERROR + "Beacons can only be created in space worlds")
                    return true
                }
                val space = sender.world
                var height = space.maxHeight.toDouble()
                if (Settings.IsV1_17) {
                    height -= space.minHeight.toDouble()
                }
                height /= 2.0
                if (Settings.IsV1_17)
                    height += space.minHeight.toDouble()
                height += .5
                if (locationMap.containsKey(sender.uniqueId)) {
                    val origin = locationMap.remove(sender.uniqueId)!!
                    val second = sender.location.clone()
                    second.y = height
                    val originStar = StarCollection.closestStar(origin)!!
                    val destinationStar = StarCollection.closestStar(second)
                    val maxDistance = HyperspaceExpansion.instance.config.getInt("Max distance from star systems", 3000)
                    if (destinationStar == null) {
                        sender.sendMessage(COMMAND_PREFIX + ERROR + "No stars are present in this space world")
                        return true
                    }
                    if (destinationStar.loc.distance(ImmutableVector.fromLocation(second)) > destinationStar.radius() + maxDistance) {
                        sender.sendMessage(COMMAND_PREFIX + ERROR + "Beacon placement is too far from the nearest star system " + destinationStar.name)
                        return true
                    }
                    sender.sendMessage(COMMAND_PREFIX + "Successfully created hyperspace link between " + originStar.name + " and " + destinationStar.name)
                    createBeacon(origin)
                    createBeacon(second)
                    HyperspaceManager.beaconLocations.add(HyperspaceBeacon(originStar.name, origin, destinationStar.name, second))
                    HyperspaceManager.saveFile()
                    return true
                }
                val origin = sender.location.clone()
                origin.y = height
                val originStar = StarCollection.closestStar(origin)
                if (originStar == null) {
                    sender.sendMessage(COMMAND_PREFIX + ERROR + "No stars are present in this space world")
                    return true
                }
                val maxDistance = HyperspaceExpansion.instance.config.getInt("Max distance from star systems", 3000)
                if (originStar.loc.distance(ImmutableVector.fromLocation(origin)) > originStar.radius() + maxDistance) {
                    sender.sendMessage(COMMAND_PREFIX + ERROR + "Beacon placement is too far from the nearest star system " + originStar.name)
                    return true
                }
                sender.sendMessage(COMMAND_PREFIX + "Set the first location for the hyperspace beacon. Select a second location in a different start system")
                locationMap[sender.uniqueId] = origin
            } else if (args[1].equals("remove", true)) {
                if (args.size < 3) {
                    sender.sendMessage(COMMAND_PREFIX + ERROR + "You must specify a beacon link")
                    return true
                }
                val split = args[2].split("-")
                var foundBeacon : HyperspaceBeacon? = null
                for (b in HyperspaceManager.beaconLocations) {
                    if (!split[0].equals(b.originName, true) || !split[1].equals(b.destinationName, true))
                        continue
                    foundBeacon = b
                    break
                }
                if (foundBeacon == null) {
                    sender.sendMessage(COMMAND_PREFIX + ERROR + "No such beacon link exists: " + args[1])
                    return true
                }
                val queue = LinkedList<ImmutableVector>()
                val visited = HashSet<ImmutableVector>()
                queue.add(ImmutableVector.fromLocation(foundBeacon.origin))
                while (!queue.isEmpty()) {
                    val node = queue.poll()
                    if (visited.contains(node))
                        continue
                    visited.add(node)
                    for (shift in SHIFTS) {
                        val test = node.add(shift)
                        if (test.toLocation(foundBeacon.origin.world!!).block.type.name.endsWith("AIR"))
                            continue
                        queue.add(test)
                    }
                }
                visited.forEach { l -> l.toLocation(foundBeacon.origin.world!!).block.type = Material.AIR }
                queue.add(ImmutableVector.fromLocation(foundBeacon.destination))
                while (!queue.isEmpty()) {
                    val node = queue.poll()
                    if (visited.contains(node))
                        continue
                    visited.add(node)
                    for (shift in SHIFTS) {
                        val test = node.add(shift)
                        if (test.toLocation(foundBeacon.destination.world!!).block.type.name.endsWith("AIR"))
                            continue
                        queue.add(test)
                    }
                }
                visited.forEach { l -> l.toLocation(foundBeacon.destination.world!!).block.type = Material.AIR }
                sender.sendMessage(COMMAND_PREFIX + "Sucessfully removed beacon link " + foundBeacon.originName + "-" + foundBeacon.destinationName)
                HyperspaceManager.beaconLocations.remove(foundBeacon)
                HyperspaceManager.saveFile()
            } else if (args[1].equals("save", true)) {
                val selection = SelectionManager.selections[sender.uniqueId]
                if (selection == null) {
                    sender.sendMessage(COMMAND_PREFIX + ERROR + "You have no selection. Make one with your selection wand, which is a " + Settings.SelectionWand)
                    return true
                }
                val center = selection.center
                for (vec in selection) {
                    val block = vec.toLocation(sender.world).block
                    if (block.type.name.endsWith("AIR"))
                        continue
                    beaconStructure.put(vec.subtract(center), MSBlock.fromBlock(block))


                }
                val blockList = ArrayList<Map<String, Any>>()
                for (entry in beaconStructure) {
                    val serialized = HashMap<String, Any>()
                    serialized.putAll(entry.key.serialize())
                    serialized.putAll(entry.value.serialize())
                    blockList.add(serialized)
                }
                val yml = YamlConfiguration()
                yml.set("blocks", blockList)
                val file = File(HyperspaceExpansion.instance.dataFolder, "beaconstructure.yml")
                if (!file.exists())
                    file.createNewFile()
                yml.save(file)
            } else if (args[1].equals("paste", true)) {
                var dz = 0
                beaconStructure.keys.forEach { vec -> if ( vec.z < dz) dz = vec.z }
                val center = ImmutableVector.fromLocation(sender.location).add(0, dz, 0)
                createBeacon(center.toLocation(sender.world))
            }

        } else if (args[0].equals("pullout", true)) {
            if (args.size < 3) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "You must specify a player")
                return true
            }
            val player = Bukkit.getPlayer(args[1])
            if (player == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Player " + args[1] + " has either not joined or is offline")
                return true
            }
            val craft = CraftManager.getInstance().getCraftByPlayer(player)
            if (craft == null) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Player " + player.name + " is not piloting a craft")
                return true
            }
            if (craft.w != HyperspaceExpansion.instance.hyperspaceWorld) {
                sender.sendMessage(COMMAND_PREFIX + ERROR + "Player " + player.name + "'s craft is not in hyperspace")
                return true
            }
            val midpoint = craft.hitBox.midPoint
            var target = HyperspaceManager.targetLocations[midpoint]
            if (target == null) {
                target = sender.location
            }
            val coords = HyperspaceManager.randomCoords(sender, target, 100, craft.hitBox.yLength)
            val dx = coords[0] - midpoint.x
            val dy = coords[1] - midpoint.y
            val dz = coords[2] - midpoint.z
            sender.sendMessage(COMMAND_PREFIX + "Pulling " + player.name + "'s craft out of hyperspace")
            craft.translate(target.world, dx, dy, dz)
        }
        return true
    }

    private fun createBeacon(loc : Location) {
        val center = ImmutableVector.fromLocation(loc)
        for (entry in beaconStructure) {
            val b = entry.key.add(center).toLocation(loc.world!!).block
            if (Settings.IsLegacy) {
                b.type = entry.value.type
                Block::class.java.getDeclaredMethod("setData", Byte::class.java).invoke(b, entry.value.data)
            } else {
                b.blockData = entry.value.data as BlockData
            }
        }
    }

    private val SHIFTS = arrayOf(
        ImmutableVector(1,1,0),
        ImmutableVector(1,0,0),
        ImmutableVector(1,-1,0),
        ImmutableVector(-1,1,0),
        ImmutableVector(-1,0,0),
        ImmutableVector(-1,-1,0),
        ImmutableVector(0,1,1),
        ImmutableVector(0,0,1),
        ImmutableVector(0,-1,1),
        ImmutableVector(0,1,-1),
        ImmutableVector(0,0,-1),
        ImmutableVector(0,-1,-1),
        ImmutableVector(0,1,0),
        ImmutableVector(0,-1,0),
        ImmutableVector(1, 0, 1),
        ImmutableVector(-1, 0, 1),
        ImmutableVector(1, 0, -1),
        ImmutableVector(-1, 0, -1)
    )
}