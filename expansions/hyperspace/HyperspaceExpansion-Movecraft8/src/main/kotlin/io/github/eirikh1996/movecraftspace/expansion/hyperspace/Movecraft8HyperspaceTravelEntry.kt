package io.github.eirikh1996.movecraftspace.expansion.hyperspace

import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.HyperspaceManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.HyperspaceTravelEntry
import net.countercraft.movecraft.craft.Craft
import net.countercraft.movecraft.craft.PlayerCraft
import org.bukkit.Location

class Movecraft8HyperspaceTravelEntry(craft : PlayerCraft, origin : Location, destination : Location, beaconTravel : Boolean = false)
    : HyperspaceTravelEntry<PlayerCraft>(craft, origin, destination, beaconTravel) {
    init {
        (HyperspaceManager.processor as Movecraft8HyperspaceProcessor).progressBars[craft] = progressBar
    }
}


