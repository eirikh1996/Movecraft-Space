package io.github.eirikh1996.movecraftspace.expansion.factions.listener

import com.massivecraft.factions.entity.Faction
import com.massivecraft.factions.entity.FactionColl
import com.massivecraft.factions.entity.MFlag
import com.massivecraft.factions.event.EventFactionsChunksChange
import com.massivecraft.massivecore.ps.PS
import io.github.eirikh1996.movecraftspace.expansion.factions.FactionsExpansion
import io.github.eirikh1996.movecraftspace.objects.Planet
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.ERROR
import net.countercraft.movecraft.MovecraftChunk
import org.bukkit.Bukkit
import org.bukkit.Bukkit.broadcastMessage
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

object FactionsListener : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onClaim (event : EventFactionsChunksChange) {
        val newFaction = event.newFaction
        if (newFaction == FactionColl.get().none ||
            newFaction == FactionColl.get().warzone ||
            newFaction == FactionColl.get().safezone ||
            event.mPlayer.isOverriding || FactionsExpansion.AllowClaimingInOrbits)
            return
        val intersectingClaimMap = HashMap<Planet, MutableSet<PS>>()
        for (ps in event.chunks) {
            val chunk = MovecraftChunk(ps.chunkX, ps.chunkZ, ps.asBukkitWorld())
            val intersectingPlanet = PlanetCollection.intersectingOtherPlanetaryOrbit(chunk)
            if (intersectingPlanet == null)
                continue
            val psSet = intersectingClaimMap.getOrDefault(intersectingPlanet, HashSet())
            psSet.add(ps)
            intersectingClaimMap.put(intersectingPlanet, psSet)

        }
        if (intersectingClaimMap.isEmpty()) {
            return
        }
        event.mPlayer.message(COMMAND_PREFIX + ERROR + "Cannot claim as claims intersect the orbit" + if (intersectingClaimMap.size == 1) "" else "s" + " of " + intersectingClaimMap.keys.joinToString { p -> p.name })
        event.isCancelled = true
    }
}