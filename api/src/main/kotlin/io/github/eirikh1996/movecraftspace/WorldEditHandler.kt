package io.github.eirikh1996.movecraftspace

import net.countercraft.movecraft.utils.HitBox
import org.bukkit.World
import org.bukkit.entity.Player
import java.io.File

abstract class WorldEditHandler constructor(dataFolder : File) {
    val asteroidFolder = File(dataFolder, "asteroids")
    abstract fun placeAsteroids(asteroidBelt : HitBox, distance : Int, world : World, player: Player)
    abstract fun undoAsteroidBelt(player: Player)
}