package io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects

import net.countercraft.movecraft.craft.Craft
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.util.Vector

data class HyperspaceTravelEntry(val craft : Craft, val origin : Location, val destination : Location, val travelDistance : Int = 1) {
    var progress = Vector(0,0,0)
    val progressBar = Bukkit.createBossBar("", BarColor.PURPLE, BarStyle.SEGMENTED_20)
    init {
        progressBar.isVisible = true
    }
}