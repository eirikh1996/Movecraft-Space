package io.github.eirikh1996.movecraftspace.listener

import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.objects.StarCollection
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockPlaceEvent

object BlockListener : Listener {

    @EventHandler
    fun onBreak(event : BlockBreakEvent) {
        val planet = PlanetCollection.getPlanetAt(event.block.location)
        val star = StarCollection.getStarAt(event.block.location, 1)
        if (planet == null || planet.destination.equals(event.block.world) && star == null)
            return
        event.isCancelled = true
    }

    @EventHandler
    fun onPlace(event : BlockPlaceEvent) {
        val planet = PlanetCollection.getPlanetAt(event.block.location)
        val star = StarCollection.getStarAt(event.block.location, 1)
        if ((planet == null || planet.destination.equals(event.block.world)) && star == null)
            return
        event.isCancelled = true
    }

    @EventHandler
    fun onFromTo(event: BlockFromToEvent) {
        val planet = PlanetCollection.getPlanetAt(event.toBlock.location)
        val star = StarCollection.getStarAt(event.toBlock.location)
        if (planet == null || planet.destination.equals(event.block.world) && star == null)
            return
        event.isCancelled = true
    }
}