package io.github.eirikh1996.movecraftspace

import io.github.eirikh1996.Movecraft8Handler
import io.github.eirikh1996.movecraftspace.utils.MovecraftHandler
import net.countercraft.movecraft.MovecraftLocation
import net.countercraft.movecraft.craft.Craft
import net.countercraft.movecraft.craft.CraftManager
import org.bukkit.World
import org.bukkit.entity.Player

internal class Movecraft7Handler() : MovecraftHandler<Craft>() {

    override fun getWorldCraftIsIn(player: Player): World? {
        val pCraft = CraftManager.getInstance().getCraftByPlayer(player) ?: return null
        return pCraft.w
    }

    override fun setRepresentationOfHitbox(player: Player): Set<MovecraftLocation> {
        val pCraft = CraftManager.getInstance().getCraftByPlayer(player) ?: return HashSet()
        return pCraft.hitBox.asSet()
    }

    override fun getCraftTypeName(player: Player): String {
        val pCraft = CraftManager.getInstance().getCraftByPlayer(player) ?: return ""
        return pCraft.type.craftName
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
        TODO("Not yet implemented")
    }
}