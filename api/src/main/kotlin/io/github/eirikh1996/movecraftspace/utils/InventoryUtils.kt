package io.github.eirikh1996.movecraftspace.utils

import io.github.eirikh1996.movecraftspace.Settings
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object InventoryUtils {



    fun createItem(type : Material, displayName : Component, lore : List<Component> = emptyList()) : ItemStack {
        val item = ItemStack(type)
        val meta = item.itemMeta!!
        meta.customName(displayName)
        meta.lore(lore)
        item.itemMeta = meta
        return item
    }

   fun createItem(type: Material, displayName: Component, vararg lore : Component = emptyArray()) = createItem(type, displayName, lore.asList())


}