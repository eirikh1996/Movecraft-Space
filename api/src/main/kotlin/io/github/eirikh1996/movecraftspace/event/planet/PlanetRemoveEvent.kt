package io.github.eirikh1996.movecraftspace.event.planet

import io.github.eirikh1996.movecraftspace.objects.Planet
import org.bukkit.event.HandlerList

class PlanetRemoveEvent(planet : Planet, isAsync : Boolean = false) : PlanetEvent(planet, isAsync) {

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic private val handlerList = HandlerList()
        @JvmStatic fun getHandlerList() = handlerList
    }
}