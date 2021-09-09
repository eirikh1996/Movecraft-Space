package io.github.eirikh1996.movecraftspace.expansion.hyperspace.events.hyperspace

import net.countercraft.movecraft.craft.Craft
import org.bukkit.Location
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList

class HyperspaceTravelEvent(craft: Craft, val currentLocation : Location) : HyperspaceEvent(craft), Cancellable {
    private var cancelled = false
    var exitMessage = ""

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun setCancelled(cancel: Boolean) {
        this.cancelled = cancel
    }

    fun cancel(cancelMessage : String) {
        cancelled = true
        exitMessage = cancelMessage
    }

    companion object {
        @JvmStatic private val handlerList = HandlerList()
        @JvmStatic fun getHandlerList() = handlerList
    }
}