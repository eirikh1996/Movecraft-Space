package io.github.eirikh1996.movecraftspace.expansion.hyperspace.events

import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.HyperspaceBeacon
import org.bukkit.event.HandlerList

class BeaconCreateEvent(beacon: HyperspaceBeacon) : BeaconEvent(beacon) {
    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic val handlerList = HandlerList()
    }

}