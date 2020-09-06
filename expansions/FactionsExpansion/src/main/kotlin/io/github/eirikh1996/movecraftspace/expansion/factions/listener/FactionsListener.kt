package io.github.eirikh1996.movecraftspace.expansion.factions.listener

import com.massivecraft.factions.entity.FactionColl
import com.massivecraft.factions.entity.MFlag
import com.massivecraft.factions.event.EventFactionsChunksChange
import com.massivecraft.massivecore.ps.PS
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object FactionsListener : Listener {

    @EventHandler
    fun onClaim (event : EventFactionsChunksChange) {
        val newFaction = event.newFaction
        if (newFaction == FactionColl.get().none ||
            newFaction == FactionColl.get().warzone ||
            newFaction == FactionColl.get().safezone ||
            event.mPlayer.isOverriding)
            return

        for (ps in event.chunks) {
            val maxX = ps.chunkX * 16
            val minX = maxX - 15
            val maxZ = ps.chunkZ * 16
            val minZ = maxZ - 15
            for (x in minX..maxX) {
                for (y in 0..255) {
                    for (z in minZ..maxX) {
                        if (!PlanetCollection.withinPlanetOrbit(Location(ps.asBukkitWorld(),
                                x.toDouble(), y.toDouble(), z.toDouble())))
                            continue

                    }
                }
            }
            if (!PlanetCollection.withinPlanetOrbit(ps.asBukkitLocation()))
                continue
            event.mPlayer.message("Cannot claim in planetary orbits")
            event.isCancelled = true
            return
        }
    }
}