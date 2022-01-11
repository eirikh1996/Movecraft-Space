package io.github.eirikh1996.movecraftspace.expansion.hyperspace.events

import net.countercraft.movecraft.craft.Craft
import net.countercraft.movecraft.events.CraftEvent
import org.bukkit.event.HandlerList

abstract class HyperspaceEvent(craft : Craft, isAsync : Boolean = false) : CraftEvent(craft, isAsync) {

}