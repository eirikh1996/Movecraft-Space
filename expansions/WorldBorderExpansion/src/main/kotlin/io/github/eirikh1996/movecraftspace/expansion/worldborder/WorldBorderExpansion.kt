package io.github.eirikh1996.movecraftspace.expansion.worldborder

import com.wimbli.WorldBorder.Config
import com.wimbli.WorldBorder.WorldBorder
import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionState
import io.github.eirikh1996.movecraftspace.objects.Immutable2dVector
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.MSWorldBorder
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player

class WorldBorderExpansion : Expansion() {

    lateinit var worldBorder: WorldBorder
    override fun enable() {
        val wb = Bukkit.getPluginManager().getPlugin("WorldBorder")
        if (wb !is WorldBorder || !wb.isEnabled) {
            logMessage(LogMessageType.CRITICAL, "WorldBorder is required, but was not found or disabled")
            state = ExpansionState.DISABLED
            return
        }
        worldBorder = wb
    }

    override fun worldBoundrary(world: World): MSWorldBorder {
        val borderData = worldBorder.getWorldBorder(world.name) ?: return super.worldBoundrary(world)
        val shape = Config::class.java.getDeclaredField("shapeRound")
        shape.isAccessible = true
        return MSWorldBorder(Immutable2dVector(borderData.x.toInt(), borderData.z.toInt()), borderData.radiusX.toDouble(), borderData.radiusZ.toDouble(), borderData.shape ?: shape.getBoolean(null))
    }

    override fun allowedArea(p: Player, loc: Location) : Boolean {
        val borderData = worldBorder.getWorldBorder(loc.world!!.name)
        return borderData == null || borderData.insideBorder(loc)
    }
}