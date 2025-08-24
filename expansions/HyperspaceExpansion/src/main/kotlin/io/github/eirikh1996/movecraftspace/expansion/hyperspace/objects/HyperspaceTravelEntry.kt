package io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects

import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.HyperspaceManager
import net.countercraft.movecraft.craft.PilotedCraft
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.util.Vector

class HyperspaceTravelEntry(val craft : PilotedCraft, val origin : Location, val destination : Location, val beaconTravel : Boolean = false) {
    var progress = Vector(0,0,0)
    var lastTeleportTime = System.currentTimeMillis()
    var stage = Stage.WARM_UP
    val progressBar = Bukkit.createBossBar("", BarColor.PURPLE, BarStyle.SEGMENTED_20)
    init {
        progressBar.isVisible = true
    }

    enum class Stage {
        WARM_UP, TRAVEL, FINISHED
    }
}