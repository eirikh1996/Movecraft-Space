package io.github.eirikh1996.movecraftspace.event.planet

import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.Planet
import org.bukkit.World
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList

class PlanetMoveEvent(val planet : Planet, val newLocation : ImmutableVector, val newSpace : World, val isAsync : Boolean = false) : PlanetEvent(planet, isAsync) {


    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic private val handlerList = HandlerList()
        @JvmStatic fun getHandlerList() = handlerList
    }

}