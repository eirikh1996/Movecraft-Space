package io.github.eirikh1996.movecraftspace

import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.Planet
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand

/**
 * @param planet The planet to move
 * @param displacement The vector to which the planet is moved
 * @param moon Determines if the plannet is a moon orbiting another planet and should be moved with the orbiting planet
 */
class PlanetMoveUpdateCommand(val planet: Planet, val displacement : ImmutableVector, private val moon : Boolean = false) : UpdateCommand() {




    override fun doUpdate() {
        planet.move(displacement, moon)
    }


}