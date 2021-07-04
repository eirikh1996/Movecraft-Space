package io.github.eirikh1996.movecraftspace

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object Settings {
    val MinimumTickRate : Double get() { return ConfigHolder.config.getDouble("MinimumTickRate", 13.0) }
    val MinimumDistanceBetweenOrbits : Int get() { return ConfigHolder.config.getInt("MinimumDistanceBetweenOrbits", 3000) }
    val MaximumDistanceBetweenOrbits : Int get() { return ConfigHolder.config.getInt("MaximumDistanceBetweenOrbits", 15000) }
    val MinimumDistanceBetweenStars : Int get() { return ConfigHolder.config.getInt("MinimumDistanceBetweenStars", 130000) }
    val MinMoonSpacing : Int get() { return ConfigHolder.config.getInt("MinMoonSpacing", 500) }
    val MaxMoonSpacing : Int get() { return ConfigHolder.config.getInt("MaxMoonSpacing", 3500) }
    val AllowPlayersTeleportationToPlanets : Boolean get() { return ConfigHolder.config.getBoolean("AllowPlayersTeleportationToPlanets", true) }
    var IsLegacy = false
    var IsV1_17 = false
    val PlanetaryRotationCheckCooldown : Int get() { return ConfigHolder.config.getInt("PlanetaryRotationCheckCooldown", 10) }
    val ExplodeSinkingCraftsInWorlds : Set<String> get() { return ConfigHolder.config.getStringList("ExplodeSinkingCraftsInWorlds").toHashSet() }
    val RotatePlanets : Boolean get() { return ConfigHolder.config.getBoolean("RotatePlanets", true) }
    val DisableReleaseInPlanetaryOrbits : Boolean get() = ConfigHolder.config.getBoolean("DisableReleaseInPlanetaryOrbits", true)
    val SelectionWand : Material get() { return Material.getMaterial(ConfigHolder.config.getString("SelectionTool","STONE_HOE")!!)!!}
}