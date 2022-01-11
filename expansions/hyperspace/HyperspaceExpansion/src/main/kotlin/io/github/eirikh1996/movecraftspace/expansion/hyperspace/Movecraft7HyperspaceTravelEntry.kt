package io.github.eirikh1996.movecraftspace.expansion.hyperspace

import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.HyperspaceManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.HyperspaceTravelEntry
import net.countercraft.movecraft.craft.Craft
import org.bukkit.Location

class Movecraft7HyperspaceTravelEntry (craft : Craft, origin : Location, destination : Location, beaconTravel : Boolean = false) : HyperspaceTravelEntry<Craft>(craft, origin, destination, beaconTravel) {
    init {
        (HyperspaceManager.processor as Movecraft7HyperspaceProcessor).progressBars[craft] = progressBar
    }
}