package io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects

import org.bukkit.entity.Player

abstract class MovecraftProcessor<C> {
    abstract fun executePulloutCommand(sender: Player, args: Array<out String>)
    abstract fun executeTravelCommand(sender: Player, args: Array<out String>)
    abstract fun craftsInHyperspaceWorld() : List<String>

}