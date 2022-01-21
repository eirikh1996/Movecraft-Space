package io.github.eirikh1996

import io.github.eirikh1996.movecraftspace.utils.MovecraftHandler
import net.countercraft.movecraft.MovecraftLocation
import net.countercraft.movecraft.craft.Craft
import net.countercraft.movecraft.craft.CraftManager
import net.countercraft.movecraft.craft.type.CraftType
import org.bukkit.World
import org.bukkit.entity.Player

class Movecraft8Handler() : MovecraftHandler<Craft>() {
    override fun getWorldCraftIsIn(player: Player): World? {
        val craft = CraftManager.getInstance().getCraftByPlayer(player) ?: return null
        return craft.world
    }

    override fun setRepresentationOfHitbox(player: Player): Set<MovecraftLocation> {
        val pCraft = CraftManager.getInstance().getCraftByPlayer(player) ?: return HashSet()
        return pCraft.hitBox.asSet()
    }

    override fun getCraftTypeName(player: Player): String {
        val pCraft = CraftManager.getInstance().getCraftByPlayer(player) ?: return ""
        return pCraft.type.getStringProperty(CraftType.NAME)
    }

    override fun getMidpointOnCraft(player: Player): MovecraftLocation {
        val pCraft = CraftManager.getInstance().getCraftByPlayer(player) ?: throw UnsupportedOperationException("Player " + player.name + "is not in command of a craft")
        return pCraft.hitBox.midPoint
    }

    override fun getCraft(player: Player): CraftHolder<Craft>? {
        val pCraft = CraftManager.getInstance().getCraftByPlayer(player) ?: return null
        return ICraftHolder(pCraft, player)
    }
    class ICraftHolder(craft: Craft, player: Player) : CraftHolder<Craft>(craft, player)

    override fun stopCruisingCraft(player: Player) {
        val pCraft = CraftManager.getInstance().getCraftByPlayer(player) ?: return
        pCraft.cruising = false
    }


}