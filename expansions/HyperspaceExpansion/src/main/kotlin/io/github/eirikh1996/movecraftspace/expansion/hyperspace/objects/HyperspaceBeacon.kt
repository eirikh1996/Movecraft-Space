package io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects

import org.bukkit.Location

data class HyperspaceBeacon (val originName : String, val origin : Location,val destinationName : String, val destination : Location) {

    override fun equals(other: Any?): Boolean {
        if (other !is HyperspaceBeacon)
            return false
        return other.origin == origin && other.originName == originName && other.destination == destination && other.destinationName == destinationName
    }

    override fun hashCode(): Int {
        return origin.hashCode() + originName.hashCode() + destination.hashCode() + destinationName.hashCode()
    }
}