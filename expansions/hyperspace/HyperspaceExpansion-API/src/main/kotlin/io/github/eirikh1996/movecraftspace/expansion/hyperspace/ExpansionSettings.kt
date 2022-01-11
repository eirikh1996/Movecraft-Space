package io.github.eirikh1996.movecraftspace.expansion.hyperspace

import org.bukkit.Material
import org.bukkit.World

object ExpansionSettings {
    lateinit var hyperspaceWorld : World
    lateinit var hyperspaceChargeSound : String
    lateinit var hyperspaceEnterSound : String
    lateinit var hyperspaceExitSound : String
    lateinit var hypermatter : Material
    lateinit var hypermatterName : String
    lateinit var allowedCraftTypesForHyperspaceSign : Set<String>
    lateinit var allowedCraftTypesForJumpCommand : Set<String>
    var extraMassShadowRangeOfPlanets = 0
    var extraMassShadowRangeOfStars = 0
    val maxHyperdrivesOnCraft = HashMap<String, Int>()
    val maxGravityWellsOnCraft = HashMap<String, Int>()


}