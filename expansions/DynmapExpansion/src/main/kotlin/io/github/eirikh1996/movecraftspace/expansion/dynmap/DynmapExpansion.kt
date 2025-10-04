package io.github.eirikh1996.movecraftspace.expansion.dynmap

import io.github.eirikh1996.movecraftspace.event.planet.PlanetMoveEvent
import io.github.eirikh1996.movecraftspace.expansion.Expansion
import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.expansion.ExpansionState
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.HyperspaceExpansion
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.HyperspaceManager
import io.github.eirikh1996.movecraftspace.objects.ImmutableVector
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.objects.StarCollection
import net.countercraft.movecraft.util.Pair
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.scheduler.BukkitRunnable
import org.dynmap.DynmapCommonAPI
import org.dynmap.markers.CircleMarker
import org.dynmap.markers.Marker

class DynmapExpansion : Expansion(), Listener {
    val orbitMarkerByID = HashMap<String, CircleMarker>()
    val planetMarkerByID = HashMap<String, Marker>()
    val beaconMarkerByID = HashMap<String, Marker>()
    val starMarkerByID = HashMap<String, Marker>()
    val previousPlanetLocations = HashMap<String, Pair<ImmutableVector, ImmutableVector>>()

    var hyperspaceExpansion: HyperspaceExpansion? = null
    override fun allowedArea(p: Player, loc: Location): Boolean {
        return true
    }

    override fun enable() {
        val dynmap = Bukkit.getPluginManager().getPlugin("dynmap")
        if (dynmap !is DynmapCommonAPI || !dynmap.isEnabled) {
            logger.severe("Dynmap is required, but was not found or disabled")
            state = ExpansionState.DISABLED
            return
        }
        val hsExpansion = ExpansionManager.getExpansion("HyperspaceExpansion")
        if (hsExpansion is HyperspaceExpansion && hsExpansion.state == ExpansionState.ENABLED) {
            hyperspaceExpansion = hsExpansion
        }
        Bukkit.getPluginManager().registerEvents(this, plugin)

        object : BukkitRunnable() {
            val planetMarkers =
                dynmap.markerAPI.getMarkerSet("planets") ?: dynmap.markerAPI.createMarkerSet(
                    "planets",
                    "Planets",
                    dynmap.markerAPI.markerIcons,
                    false
                )
            val orbitMarkers =
                dynmap.markerAPI.getMarkerSet("orbits") ?: dynmap.markerAPI.createMarkerSet(
                    "orbits",
                    "Orbits",
                    dynmap.markerAPI.markerIcons,
                    false
                )
            val sMarkers = dynmap.markerAPI.getMarkerSet("stars")
            val starMarkers = if (sMarkers == null) {
                dynmap.markerAPI.createMarkerSet("stars", "Stars", dynmap.markerAPI.markerIcons, false)
            } else {
                sMarkers
            }
            val bMarkers = dynmap.markerAPI.getMarkerSet("hyperspace beacons")
            val beaconMarkers = if (bMarkers == null) {
                dynmap.markerAPI.createMarkerSet(
                    "hyperspace beacons",
                    "Hyperspace beacons",
                    dynmap.markerAPI.markerIcons,
                    false
                )
            } else {
                bMarkers
            }

            override fun run() {
                for (planet in PlanetCollection) {
                    val star = StarCollection.closestStar(planet.orbitCenter.toBukkit(planet.space))!!
                    val orbitMarkerID = star.name + "_" + planet.destination.name + "_orbit_" + planet.space.name
                    val orbitRadius = planet.center.distance(planet.orbitCenter).toDouble()
                    val orbitMarker = if (!orbitMarkerByID.containsKey(orbitMarkerID)) {
                        val tempOrbitMarker = orbitMarkers.findCircleMarker(orbitMarkerID)
                        if (tempOrbitMarker == null) {
                            orbitMarkers.createCircleMarker(
                                orbitMarkerID, orbitMarkerID, false, planet.space.name,
                                planet.orbitCenter.x.toDouble(),
                                planet.orbitCenter.y.toDouble(),
                                planet.orbitCenter.z.toDouble(), orbitRadius, orbitRadius, false
                            )
                        } else {
                            tempOrbitMarker.setCenter(
                                planet.space.name,
                                planet.orbitCenter.x.toDouble(),
                                planet.orbitCenter.y.toDouble(),
                                planet.orbitCenter.z.toDouble()
                            )
                            tempOrbitMarker.setRadius(orbitRadius, orbitRadius)
                            tempOrbitMarker
                        }
                    } else {
                        val tempOrbitMarker = orbitMarkerByID[orbitMarkerID]!!
                        tempOrbitMarker.setCenter(
                            planet.space.name,
                            planet.orbitCenter.x.toDouble(),
                            planet.orbitCenter.y.toDouble(),
                            planet.orbitCenter.z.toDouble()
                        )
                        tempOrbitMarker.setRadius(orbitRadius, orbitRadius)
                        tempOrbitMarker
                    }
                    orbitMarker.setFillStyle(0.0, 16711680)
                    val planetMarkerID = planet.destination.name + "_planet_" + planet.space.name
                    val markerIcon = dynmap.markerAPI.getMarkerIcon("world")
                    val planetMarker = if (!planetMarkerByID.containsKey(planetMarkerID)) {
                        val tempPlanetMarker = planetMarkers.findMarker(planetMarkerID)
                        if (tempPlanetMarker == null) {
                            planetMarkers.createMarker(
                                planetMarkerID, planet.destination.name, planet.space.name,
                                planet.center.x.toDouble(),
                                planet.center.y.toDouble(), planet.center.z.toDouble(), markerIcon, false
                            )
                        } else {
                            tempPlanetMarker.setLocation(
                                planet.space.name,
                                planet.center.x.toDouble(), planet.center.y.toDouble(), planet.center.z.toDouble()
                            )
                            tempPlanetMarker.setMarkerIcon(markerIcon)
                            tempPlanetMarker
                        }
                    } else {
                        val tempPlanetMarker = planetMarkerByID.get(planetMarkerID)!!
                        tempPlanetMarker.setLocation(
                            planet.space.name,
                            planet.center.x.toDouble(), planet.center.y.toDouble(), planet.center.z.toDouble()
                        )
                        tempPlanetMarker.markerIcon = markerIcon
                        tempPlanetMarker
                    }
                    if (planetMarker != null && !planetMarkerByID.containsKey(planetMarkerID)) {
                        planetMarkerByID[planetMarkerID] = planetMarker
                    }
                    if (orbitMarker != null && !orbitMarkerByID.containsKey(orbitMarkerID)) {
                        orbitMarkerByID[orbitMarkerID] = orbitMarker
                    }
                    if (previousPlanetLocations.containsKey(planet.name)) {
                        val minmax = previousPlanetLocations[planet.name]
                        if (minmax != null) {
                            val min = minmax.left
                            val max = minmax.right
                            dynmap.triggerRenderOfVolume(planet.space.name, min.x, min.y, min.z, max.x, max.y, max.z)
                        }
                    }
                    dynmap.triggerRenderOfVolume(planet.space.name, planet.minX, planet.minY, planet.minZ, planet.maxX, planet.minY, planet.maxz)
                }
                for (star in StarCollection) {

                    val starMarkerID = star.name + "_" + star.space.name
                    val starMarkerIcon = dynmap.markerAPI.getMarkerIcon("sun")
                    val starMarker = if (!starMarkerByID.containsKey(starMarkerID)) {
                        val tempStarMarker = starMarkers.findMarker(starMarkerID)
                        if (tempStarMarker == null) {
                            starMarkers.createMarker(
                                starMarkerID, star.name, star.space.name,
                                star.loc.x.toDouble(), star.loc.y.toDouble(),
                                star.loc.z.toDouble(), starMarkerIcon, false
                            )
                        } else {
                            tempStarMarker.setLocation(
                                star.space.name, star.loc.x.toDouble(),
                                star.loc.y.toDouble(), star.loc.z.toDouble()
                            )
                            tempStarMarker.setMarkerIcon(starMarkerIcon)
                            tempStarMarker
                        }
                    } else {
                        val tempStarMarker = starMarkerByID.get(starMarkerID)!!
                        tempStarMarker.setLocation(
                            star.space.name, star.loc.x.toDouble(), star.loc.y.toDouble(),
                            star.loc.z.toDouble()
                        )
                        tempStarMarker.setMarkerIcon(starMarkerIcon)
                        tempStarMarker
                    }
                    if (starMarker != null && !starMarkerByID.containsKey(starMarkerID)) {
                        starMarkerByID.put(starMarkerID, starMarker)
                    }
                    dynmap.triggerRenderOfVolume(star.space.name, star.loc.x - star.radius, star.loc.y - star.radius, star.loc.z - star.radius, star.loc.x + star.radius, star.loc.y + star.radius, star.loc.z + star.radius)
                }
                for (planetMarker in planetMarkerByID.keys) {
                    val marker = planetMarkerByID[planetMarker]!!
                    if (PlanetCollection.getPlanetByName(marker.label) != null)
                        continue
                    planetMarkerByID.remove(planetMarker)
                    marker.deleteMarker()
                }
                for (orbitMarker in orbitMarkerByID.keys) {
                    val planetName = orbitMarker.split("_")[1]
                    if (PlanetCollection.getPlanetByName(planetName) != null) {
                        continue
                    }
                    orbitMarkerByID.remove(orbitMarker)!!.deleteMarker()
                }
                for (starMarker in starMarkerByID.keys) {
                    val marker = starMarkerByID[starMarker]!!
                    if (StarCollection.getStarByName(marker.label) != null)
                        continue
                    starMarkerByID.remove(starMarker)
                    marker.deleteMarker()

                }
                if (hyperspaceExpansion == null) {
                    return
                }
                for (hsBeacon in HyperspaceManager.beaconLocations) {
                    val beaconMarker = dynmap.markerAPI.getMarkerIcon("portal")
                    val originBeaconMarkerID = hsBeacon.originName + "_" + hsBeacon.destinationName
                    val originMarker = if (!beaconMarkerByID.containsKey(originBeaconMarkerID)) {
                        val tempOriginBeaconMarker = beaconMarkers.findMarker(originBeaconMarkerID)
                        if (tempOriginBeaconMarker == null) {
                            beaconMarkers.createMarker(
                                originBeaconMarkerID,
                                originBeaconMarkerID,
                                hsBeacon.origin.world!!.name,
                                hsBeacon.origin.x,
                                hsBeacon.origin.y,
                                hsBeacon.origin.z,
                                beaconMarker,
                                false
                            )
                        } else {
                            tempOriginBeaconMarker.setLocation(
                                hsBeacon.origin.world!!.name,
                                hsBeacon.origin.x,
                                hsBeacon.origin.y,
                                hsBeacon.origin.z
                            )
                            tempOriginBeaconMarker.setMarkerIcon(beaconMarker)
                            tempOriginBeaconMarker
                        }
                    } else {
                        val tempOriginBeaconMarker = beaconMarkerByID[originBeaconMarkerID]!!
                        tempOriginBeaconMarker.setLocation(
                            hsBeacon.origin.world!!.name,
                            hsBeacon.origin.x,
                            hsBeacon.origin.y,
                            hsBeacon.origin.z
                        )
                        tempOriginBeaconMarker.setMarkerIcon(beaconMarker)
                        tempOriginBeaconMarker
                    }
                    if (originMarker != null && !beaconMarkerByID.containsKey(originBeaconMarkerID)) {
                        beaconMarkerByID.put(originBeaconMarkerID, originMarker)
                    }
                    val destinationBeaconMarkerID = hsBeacon.destinationName + "_" + hsBeacon.originName
                    val destinationMarker = if (!beaconMarkerByID.containsKey(destinationBeaconMarkerID)) {
                        val tempDestinationBeaconMarker = beaconMarkers.findMarker(destinationBeaconMarkerID)
                        if (tempDestinationBeaconMarker == null) {
                            beaconMarkers.createMarker(
                                destinationBeaconMarkerID,
                                destinationBeaconMarkerID,
                                hsBeacon.destination.world!!.name,
                                hsBeacon.destination.x,
                                hsBeacon.destination.y,
                                hsBeacon.destination.z,
                                beaconMarker,
                                false
                            )
                        } else {
                            tempDestinationBeaconMarker.setLocation(
                                hsBeacon.destination.world!!.name,
                                hsBeacon.destination.x,
                                hsBeacon.destination.y,
                                hsBeacon.destination.z
                            )
                            tempDestinationBeaconMarker.setMarkerIcon(beaconMarker)
                            tempDestinationBeaconMarker
                        }
                    } else {
                        val tempDestinationBeaconMarker = beaconMarkerByID[destinationBeaconMarkerID]!!
                        tempDestinationBeaconMarker.setLocation(
                            hsBeacon.destination.world!!.name,
                            hsBeacon.destination.x,
                            hsBeacon.destination.y,
                            hsBeacon.destination.z
                        )
                        tempDestinationBeaconMarker.setMarkerIcon(beaconMarker)
                        tempDestinationBeaconMarker
                    }
                    if (destinationMarker != null && !beaconMarkerByID.containsKey(destinationBeaconMarkerID)) {
                        beaconMarkerByID.put(destinationBeaconMarkerID, destinationMarker)
                    }
                }

            }


        }.runTaskTimerAsynchronously(plugin, 0, 200)
    }

    @EventHandler
    fun onPlanetMove(event : PlanetMoveEvent) {
        val oldLoc = event.planet.center
        val radius = event.planet.radius
        val minMaxPair = Pair(
            ImmutableVector(oldLoc.x - radius, oldLoc.y - radius, oldLoc.z - radius),
            ImmutableVector(oldLoc.x + radius, oldLoc.y + radius, oldLoc.z + radius)
        )
        previousPlanetLocations[event.planet.name] = minMaxPair
    }
}