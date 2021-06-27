package io.github.eirikh1996.movecraftspace.event.expansion

import io.github.eirikh1996.movecraftspace.expansion.Expansion
import org.bukkit.event.HandlerList

class ExpansionEnableEvent constructor(expansion: Expansion) : ExpansionEvent(expansion) {

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }

    companion object {
        @JvmStatic val HANDLERS = HandlerList()
        @JvmStatic fun getHandlerList() = HANDLERS
    }
}