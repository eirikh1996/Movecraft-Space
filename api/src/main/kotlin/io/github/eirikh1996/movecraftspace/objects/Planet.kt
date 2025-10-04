package io.github.eirikh1996.movecraftspace.objects

import io.github.eirikh1996.movecraftspace.event.planet.PlanetMoveEvent
import net.countercraft.movecraft.Movecraft
import net.countercraft.movecraft.MovecraftLocation
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.data.BlockData
import java.util.*
import kotlin.collections.HashSet
import kotlin.math.min

class Planet(
    var center: MovecraftLocation,
    var orbitCenter : MovecraftLocation,
    val radius : Int,
    space : World,
    val destination : World,
    val orbitTime : Int,
    val exitHeight : Int
) {
    var space : World = space
        get() = field
        private set(value) {
            field = value
        }
    var moving = false
    val name : String get() { return destination.name }
    val moons = HashSet<Planet>()
    val id: UUID = UUID.randomUUID()
    fun contains(location: Location, extraRadius : Int = 0) : Boolean {
        if (space != location.world) {
            return false
        }
        return center.distance(MovecraftLocation(location.blockX, location.blockY, location.blockZ)) <= radius + extraRadius
    }

    fun isPlanet(world: World) : Boolean {
        return destination == world
    }

    fun moonOrbitRadius(): Int {
        if (moons.isEmpty())
            return radius
        var orbitRadius = radius
        for (m in moons) {
            if (m.center.distance(center).toInt() < orbitRadius)
                continue
            orbitRadius = m.center.distance(center).toInt()
        }
        return orbitRadius
    }

    fun isMoon() : Boolean {
        for (pl in PlanetCollection) {
            if (pl.moons.contains(this))
                return true
        }
        return false
    }
    override fun hashCode(): Int {
        return destination.hashCode() + space.hashCode() + id.hashCode()
    }

    var minX = center.x - radius
    var maxX = center.x + radius
    var minY = center.y - radius
    var maxY = center.y + radius
    var minZ = center.z - radius
    var maxz = center.z + radius

    override fun equals(other: Any?): Boolean {
        if (other !is Planet) {
            return false
        }
        return destination == other.destination && space == other.space && id.equals(other.id)
    }

    fun move(displacement : MovecraftLocation, moon : Boolean = false, newSpace : World = space) {
        val newCenter = center.add(displacement)
        //call event
        val event = PlanetMoveEvent(this, newCenter, newSpace)
        Bukkit.getPluginManager().callEvent(event)
        moving = true
        var type = center.toBukkit(space).block.type
        var y = center.y
        val maxHeight = space.maxHeight
        while (type.isAir && y <= maxHeight) {
            y++
            type = space.getBlockAt(center.x, min(y, maxHeight), center.z).type
        }
        val start = MovecraftLocation(center.x, y, center.z)
        val queue = LinkedList<MovecraftLocation>()
        val visited = HashSet<MovecraftLocation>()
        queue.add(start)
        while (!queue.isEmpty()) {
            val node = queue.poll()
            if (visited.contains(node))
                continue
            visited.add(node)
            for (shift in SHIFTS) {
                val test = node.add(shift)
                if (test.toBukkit(space).block.type.isAir)
                    continue
                queue.add(test)
            }
        }

        val newLocMap = HashMap<MovecraftLocation, BlockData>()
        for (loc in visited) {
            val b = loc.toBukkit(space).block
            newLocMap[loc.add(displacement)] = b.blockData
        }
        for (loc in newLocMap.keys) {
            val data = newLocMap[loc]!!
            Movecraft.getInstance().worldHandler.setBlockFast(loc.toBukkit(space), data)
        }
        for (loc in visited.filter { vec -> !newLocMap.containsKey(vec) }) {
            Movecraft.getInstance().worldHandler.setBlockFast(loc.toBukkit(space), Bukkit.createBlockData(Material.AIR))
        }
        center = newCenter
        space = newSpace
        if (moon)
            orbitCenter = orbitCenter.add(displacement)
        minX = center.x - radius
        maxX = center.x + radius
        minY = center.y - radius
        maxY = center.y + radius
        minZ = center.z - radius
        maxz = center.z + radius
        PlanetCollection.saveFile()
        moving = false;
    }

    var orbitRadius : Int get() { return center.distance(orbitCenter).toInt() } set(value) { throw UnsupportedOperationException()}


    private val SHIFTS = arrayOf(
        MovecraftLocation(1,1,0),
        MovecraftLocation(1,0,0),
        MovecraftLocation(1,-1,0),
        MovecraftLocation(-1,1,0),
        MovecraftLocation(-1,0,0),
        MovecraftLocation(-1,-1,0),
        MovecraftLocation(0,1,1),
        MovecraftLocation(0,0,1),
        MovecraftLocation(0,-1,1),
        MovecraftLocation(0,1,-1),
        MovecraftLocation(0,0,-1),
        MovecraftLocation(0,-1,-1),
        MovecraftLocation(0,1,0),
        MovecraftLocation(0,-1,0)
    )
}