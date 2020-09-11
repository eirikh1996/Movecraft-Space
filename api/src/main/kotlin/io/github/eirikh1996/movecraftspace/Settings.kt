package io.github.eirikh1996.movecraftspace

object Settings {
    val MinimumTickRate : Double get() { return ConfigHolder.config["MinimumTickRate", 13.0] as Double }
    val MinimumDistanceBetweenOrbits : Int get() { return ConfigHolder.config["MinimumDistanceBetweenOrbits", 3000] as Int }
    val MaximumDistanceBetweenOrbits : Int get() { return ConfigHolder.config["MaximumDistanceBetweenOrbits", 15000] as Int }
    val MinimumDistanceBetweenStars : Int get() { return ConfigHolder.config["MinimumDistanceBetweenStars", 130000] as Int }
    val MinMoonSpacing : Int get() { return ConfigHolder.config["MinMoonSpacing", 500] as Int }
    val MaxMoonSpacing : Int get() { return ConfigHolder.config["MaxMoonSpacing", 3500] as Int }
    var IsLegacy = false
    val WorldSwitchCooldownTime : Int get() { return ConfigHolder.config["WorldSwitchCooldownTime", 10] as Int }
    val ExplodeSinkingCraftsInWorlds : Set<String> get() { return (ConfigHolder.config["ExplodeSinkingCraftsInWorlds", ArrayList<String>()] as List<String>).toHashSet() }
    val RotatePlanets : Boolean get() { return ConfigHolder.config["RotatePlanets", true] as Boolean }
}