package io.github.eirikh1996.movecraftspace.expansion.worldborder

import com.wimbli.WorldBorder.WorldBorder
import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionState
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

    override fun worldBoundrary(world: World): IntArray {
        val borderData = worldBorder.getWorldBorder(world.name)
        if (borderData == null) {
            return super.worldBoundrary(world)
        }

        return intArrayOf((borderData.x - borderData.radiusX).toInt(), (borderData.x + borderData.radiusX).toInt(), (borderData.z - borderData.radiusZ).toInt(), (borderData.z + borderData.radiusZ).toInt())
    }

    override fun allowedArea(p: Player, loc: Location) : Boolean {
        val borderData = worldBorder.getWorldBorder(loc.world!!.name)
        return borderData == null || borderData.insideBorder(loc)
    }
}