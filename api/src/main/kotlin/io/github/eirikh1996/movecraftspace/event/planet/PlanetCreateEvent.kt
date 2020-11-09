package io.github.eirikh1996.movecraftspace.event.planet

import io.github.eirikh1996.movecraftspace.objects.Planet
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList

class PlanetCreateEvent(planet : Planet, isAsync : Boolean = false) : PlanetEvent(planet, isAsync), Cancellable {
    private var isCancelled = false

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic private val handlerList = HandlerList()
        @JvmStatic fun getHandlerList() = handlerList
    }

    override fun isCancelled(): Boolean {
        return isCancelled
    }

    override fun setCancelled(cancel: Boolean) {
        isCancelled = cancel
    }
}