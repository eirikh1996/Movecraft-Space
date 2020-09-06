package io.github.eirikh1996.movecraftspace.listener

import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.entity.EntityExplodeEvent

object ExplosionListener : Listener {

    @EventHandler
    fun blockExplode(event : BlockExplodeEvent) {
        val iter = event.blockList().iterator()
        while (iter.hasNext()) {
            val next = iter.next()
            val planet = PlanetCollection.getPlanetAt(next.location)
            if (planet == null || planet.destination.equals(next.world))
                continue
            iter.remove()
        }
    }

    @EventHandler
    fun entityExplode(event : EntityExplodeEvent) {
        val iter = event.blockList().iterator()
        while (iter.hasNext()) {
            val next = iter.next()
            val planet = PlanetCollection.getPlanetAt(next.location)
            if (planet == null || planet.destination.equals(next.world))
                continue
            iter.remove()
        }
    }
}