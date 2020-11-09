package io.github.eirikh1996.movecraftspace.event.planet

import io.github.eirikh1996.movecraftspace.objects.Planet
import org.bukkit.event.Event

abstract class PlanetEvent(planet : Planet, isAsync : Boolean = false) : Event(isAsync) {

}