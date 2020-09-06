package io.github.eirikh1996.movecraftspace

import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.Planet
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import net.countercraft.movecraft.Movecraft
import net.countercraft.movecraft.MovecraftLocation
import net.countercraft.movecraft.WorldHandler
import net.countercraft.movecraft.craft.CraftManager
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand
import net.countercraft.movecraft.utils.BitmapHitBox
import net.countercraft.movecraft.utils.SolidHitBox
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import java.lang.Exception
import java.lang.reflect.Method
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.min

/**
 * @param planet The planet to move
 * @param displacement The vector to which the planet is moved
 * @param moon Determines if the plannet is a moon orbiting another planet and should be moved with the orbiting planet
 */
class PlanetMoveUpdateCommand constructor(val planet: Planet, val displacement : ImmutableVector, val moon : Boolean = false) : UpdateCommand() {




    override fun doUpdate() {
        planet.move(displacement, moon)
    }


}