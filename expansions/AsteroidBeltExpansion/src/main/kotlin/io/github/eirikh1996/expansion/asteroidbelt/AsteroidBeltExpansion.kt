package io.github.eirikh1996.expansion.asteroidbelt

import io.github.eirikh1996.expansion.asteroidbelt.`object`.Asteroid
import io.github.eirikh1996.expansion.asteroidbelt.command.AsteroidBeltCommand
import io.github.eirikh1996.expansion.asteroidbelt.command.AsteroidCommand
import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.selection.SelectionSupported
import java.io.File
import java.io.FileFilter
import kotlin.properties.Delegates

class AsteroidBeltExpansion : Expansion(), SelectionSupported {

    val asteroids = HashMap<String, Asteroid>()
    var distanceBetweenAsteroids by Delegates.notNull<Int>()

    override fun load() {
        instance = this
    }

    override fun enable() {
        saveDefaultConfig()
        distanceBetweenAsteroids = config.getInt("Distance between asteroids", 20)
        val asteroidDir = File(dataFolder, "asteroids")
        if (!asteroidDir.exists())
            asteroidDir.mkdirs()
        asteroidDir.listFiles { file -> file != null && file.name.endsWith(".yml") }?.forEach { file ->
            val asteroid = Asteroid.loadFromFile(file)
            asteroids[asteroid.name.lowercase()] = asteroid
        }
        logMessage(LogMessageType.INFO, "Loaded ${asteroids.size} asteroids")
        plugin.getCommand("asteroid")?.setExecutor(AsteroidCommand)
        plugin.getCommand("asteroidbelt")?.setExecutor(AsteroidBeltCommand)
    }

    companion object {
        lateinit var instance : AsteroidBeltExpansion
    }

}