package io.github.eirikh1996.movecraftspace.utils

import io.github.eirikh1996.movecraftspace.Settings
import net.countercraft.movecraft.MovecraftLocation
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player

abstract class MovecraftHandler<C> {
    private val craftClass = Class.forName("net.countercraft.movecraft.craft.Craft");
    private val hitBoxClass = Class.forName("net.countercraft.movecraft.util" + (if (Settings.IsMovecraft8) ".hitboxes" else "s") + ".HitBox")
    private val translateMethod = craftClass.getDeclaredMethod("translate", World::class.java, Int::class.java, Int::class.java, Int::class.java)
    private val getHitBoxMethod = craftClass.getDeclaredMethod("getHitBox")
    private val getMidPointMethod = hitBoxClass.getDeclaredMethod("getMidPoint")
    private val asSetMethod = hitBoxClass.getDeclaredMethod("asSet")

    init {

    }

    fun teleportCraft(craftHolder: CraftHolder<C>, target : Location) {
        val craft = craftHolder.craft ?: throw IllegalArgumentException("parameter \"craft\" cannot be null")
        if (!craftClass.isAssignableFrom((craft as Any)::class.java)) {
            throw IllegalArgumentException("parameter \"craft\" is neither the same as nor a subclass of " + craftClass.name)
        }
        val hitbox = getHitBoxMethod.invoke(craft)
        val center = getMidPointMethod.invoke(hitbox) as MovecraftLocation
        translateMethod.invoke(craft, target.world, target.blockX - center.x, target.blockY - center.y, target.blockZ - center.z)
    }

    abstract fun setRepresentationOfHitbox(player: Player) : Set<MovecraftLocation>

    abstract fun getWorldCraftIsIn(player: Player) : World?

    abstract fun getCraftTypeName(player: Player) : String

    abstract fun getMidpointOnCraft(player: Player) : MovecraftLocation

    abstract fun getCraft(player: Player) : CraftHolder<C>?

    abstract fun stopCruisingCraft(player: Player)

    companion object {
        var instance : MovecraftHandler<*>? = null
            get() {
            if (field == null) {
                val clazz = Class.forName("io.github.eirikh1996.movecraftspace.Movecraft" + if (Settings.IsMovecraft8) "8" else "7" + "Handler")
                val constructor = clazz.getConstructor()
                constructor.isAccessible = true
                field = constructor.newInstance() as MovecraftHandler<*>
            }
            return field
        } set(value) {
            throw UnsupportedOperationException()
        }
    }

    abstract class CraftHolder<C>(val craft: C, val player: Player) {

    }
}