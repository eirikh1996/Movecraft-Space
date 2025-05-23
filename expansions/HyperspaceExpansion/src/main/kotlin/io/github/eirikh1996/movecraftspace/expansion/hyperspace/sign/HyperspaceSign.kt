package io.github.eirikh1996.movecraftspace.expansion.hyperspace.sign

import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent

object HyperspaceSign : Listener {
    val HEADER = "§bHyperspace"
    @EventHandler
    fun onChange(e : SignChangeEvent) {
        if (!ChatColor.stripColor(e.getLine(0))!!.equals("[hyperspace]"))
            return
        e.setLine(0, HEADER)
    }


}