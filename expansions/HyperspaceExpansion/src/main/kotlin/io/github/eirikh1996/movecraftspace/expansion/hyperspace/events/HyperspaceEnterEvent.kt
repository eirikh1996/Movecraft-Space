package io.github.eirikh1996.movecraftspace.expansion.hyperspace.events

import net.countercraft.movecraft.craft.Craft
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList

class HyperspaceEnterEvent(craft: Craft) : HyperspaceEvent(craft, true), Cancellable {
    private var cancelled = false

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun setCancelled(cancel: Boolean) {
        this.cancelled = cancel
    }

    companion object {
        @JvmStatic private val handlerList = HandlerList()
        @JvmStatic fun getHandlerList() = handlerList
    }
}