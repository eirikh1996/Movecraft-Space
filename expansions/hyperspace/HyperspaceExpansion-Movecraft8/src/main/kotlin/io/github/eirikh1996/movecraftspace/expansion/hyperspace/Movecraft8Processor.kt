package io.github.eirikh1996.movecraftspace.expansion.hyperspace

import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.HyperspaceManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.MovecraftProcessor
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils.plugin
import net.countercraft.movecraft.craft.Craft
import net.countercraft.movecraft.craft.CraftManager
import net.countercraft.movecraft.craft.PlayerCraft
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

class Movecraft8Processor : MovecraftProcessor<PlayerCraft>() {
    private val ex = ExpansionManager.getExpansion("HyperspaceExpansion")!!

    override fun executePulloutCommand(sender: Player, args: Array<out String>) {
        if (!sender.hasPermission("movecraftspace.command.hyperspace.pullout")) {
            sender.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + MSUtils.COMMAND_NO_PERMISSION)
            return
        }
        if (args.size < 2) {
            sender.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "You must specify a player")
            return
        }
        val player = Bukkit.getPlayer(args[1])
        if (player == null) {
            sender.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Player " + args[1] + " has either not joined or is offline")
            return
        }
        val craft = CraftManager.getInstance().getCraftByPlayer(player)
        if (craft == null) {
            sender.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Player " + player.name + " is not piloting a craft")
            return
        }
        if (craft.w != ExpansionSettings.hyperspaceWorld) {
            sender.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "Player " + player.name + "'s craft is not in hyperspace")
            return
        }
        val midpoint = craft.hitBox.midPoint
        var target = HyperspaceManager.targetLocations.remove(midpoint)
        if (target == null) {
            target = sender.location.clone()
            target.add(craft.hitBox.xLength.toDouble(), 0.0, craft.hitBox.zLength.toDouble())
        }
        val hsProcessor = HyperspaceManager.processor as Movecraft8HyperspaceProcessor
        target = hsProcessor.nearestUnobstructedLoc(target, craft)
        object : BukkitRunnable() {
            override fun run() {
                hsProcessor.pullOutOfHyperspace(hsProcessor.processingEntries[craft]!!, target, MSUtils.COMMAND_PREFIX + "Pulling " + player.name + "'s craft out of hyperspace")
            }
        }.runTaskAsynchronously(plugin)
    }

    override fun executeTravelCommand(sender: Player, args: Array<out String>) {
        val craft = CraftManager.getInstance().getCraftByPlayer(sender)
        if (craft == null) {
            sender.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "You are not commanding a space craft")
            return
        }
        val hitBox = craft.hitBox
        var foundLoc : Location? = null
        var destination : Location? = null
        var str = ""
        for (beacon in HyperspaceManager.beaconLocations) {
            if (beacon.origin.world!! == craft.world && beacon.origin.distance(hitBox.midPoint.toBukkit(craft.world)) <= ex.config.getInt("Beacon range")) {
                foundLoc = beacon.origin
                destination = beacon.destination
                str = beacon.originName + "-" + beacon.destinationName
                break
            }
            if (beacon.destination.world!! == craft.world && beacon.destination.distance(hitBox.midPoint.toBukkit(craft.world)) <= ex.config.getInt("Beacon range")) {
                foundLoc = beacon.destination
                destination = beacon.origin
                str = beacon.destinationName + "-" + beacon.originName
                break
            }
        }
        if (foundLoc == null) {
            sender.sendMessage(MSUtils.COMMAND_PREFIX + MSUtils.ERROR + "You are not within the range of a hyperspace beacon")
            return
        }
        (HyperspaceManager.processor as Movecraft8HyperspaceProcessor).scheduleHyperspaceTravel(craft, foundLoc, destination!!, str, true)

    }

    override fun craftsInHyperspaceWorld(): List<String> {
        val players = ArrayList<String>()
        CraftManager.getInstance().getCraftsInWorld(ExpansionSettings.hyperspaceWorld).forEach { craft -> players.add(craft.notificationPlayer?.name!!) }
        return players
    }

}