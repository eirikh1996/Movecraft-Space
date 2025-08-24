package io.github.eirikh1996.movecraftspace.objects

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.event.planet.PlanetMoveEvent
import io.github.eirikh1996.movecraftspace.utils.MSUtils.setBlock
import net.countercraft.movecraft.Movecraft
import net.countercraft.movecraft.MovecraftLocation
import net.countercraft.movecraft.WorldHandler
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.data.BlockData
import org.bukkit.util.Vector
import java.lang.Exception
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger
import kotlin.collections.HashSet
import kotlin.math.min

data class Planet(
    var center: ImmutableVector,
    var orbitCenter : ImmutableVector,
    val radius : Int,
    val space : World,
    val destination : World,
    val orbitTime : Int,
    val exitHeight : Int
) {
    var setBlockFast : Method? = null
    init {
        try {
            setBlockFast = WorldHandler::class.java.getDeclaredMethod("setBlockFast", Location::class.java, Material::class.java, Any::class.java)
        } catch (e : Exception) {

        }
    }
    var moving = false
    val name : String get() { return destination.name }
    val moons = HashSet<Planet>()
    val id: UUID = UUID.randomUUID()
    fun contains(location: Location, extraRadius : Int = 0) : Boolean {
        if (space != location.world) {
            return false
        }
        return center.distance(ImmutableVector.fromLocation(location)) <= radius + extraRadius
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

    fun move(displacement : ImmutableVector, moon : Boolean = false, newSpace : World = space) {
        val newCenter = center.add(displacement)
        //call event
        val event = PlanetMoveEvent(this, newCenter, newSpace)
        Bukkit.getPluginManager().callEvent(event)
        moving = true
        var type = center.toLocation(space).block.type
        var y = center.y
        while (type == Material.AIR && y <= 255) {
            y++
            type = space.getBlockAt(center.x, min(y, 255), center.z).type
        }
        val start = ImmutableVector(center.x, y, center.z)
        val queue = LinkedList<ImmutableVector>()
        val visited = HashSet<ImmutableVector>()
        queue.add(start)
        while (!queue.isEmpty()) {
            val node = queue.poll()
            if (visited.contains(node))
                continue
            visited.add(node)
            for (shift in SHIFTS) {
                val test = node.add(shift)
                if (test.toLocation(space).block.type.name.endsWith("AIR"))
                    continue
                queue.add(test)
            }
        }

        val newLocMap = HashMap<ImmutableVector, BlockData>()
        for (loc in visited) {
            val b = loc.toLocation(space).block
            newLocMap[loc.add(displacement)] = b.blockData
        }
        for (loc in newLocMap.keys) {
            val data = newLocMap[loc]!!
            Movecraft.getInstance().worldHandler.setBlockFast(loc.toLocation(space), data)
        }
        for (loc in visited.filter { vec -> !newLocMap.containsKey(vec) }) {
            Movecraft.getInstance().worldHandler.setBlockFast(loc.toLocation(space), Bukkit.createBlockData(Material.AIR))
        }
        center = newCenter
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
        ImmutableVector(1,1,0),
        ImmutableVector(1,0,0),
        ImmutableVector(1,-1,0),
        ImmutableVector(-1,1,0),
        ImmutableVector(-1,0,0),
        ImmutableVector(-1,-1,0),
        ImmutableVector(0,1,1),
        ImmutableVector(0,0,1),
        ImmutableVector(0,-1,1),
        ImmutableVector(0,1,-1),
        ImmutableVector(0,0,-1),
        ImmutableVector(0,-1,-1),
        ImmutableVector(0,1,0),
        ImmutableVector(0,-1,0)
    )
}