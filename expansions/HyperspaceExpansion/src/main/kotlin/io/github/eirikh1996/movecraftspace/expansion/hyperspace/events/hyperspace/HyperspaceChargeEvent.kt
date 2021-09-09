package io.github.eirikh1996.movecraftspace.expansion.hyperspace.events.hyperspace

import net.countercraft.movecraft.craft.Craft
import org.bukkit.event.HandlerList

class HyperspaceChargeEvent(craft: Craft, charge : Double) : HyperspaceEvent(craft, true) {
    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic private val handlerList = HandlerList()
        @JvmStatic fun getHandlerList() = handlerList
    }
}