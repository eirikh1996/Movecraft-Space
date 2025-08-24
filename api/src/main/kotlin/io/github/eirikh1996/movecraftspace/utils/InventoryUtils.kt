package io.github.eirikh1996.movecraftspace.utils

import io.github.eirikh1996.movecraftspace.Settings
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object InventoryUtils {



    fun createItem(type : Material, displayName : String, lore : List<String> = emptyList()) : ItemStack {
        val item = ItemStack(type)
        val meta = item.itemMeta!!
        meta.setDisplayName(displayName)
        meta.lore = lore
        return item
    }

   fun createItem(type: Material, displayName: String, vararg lore : String = emptyArray()) = createItem(type, displayName, lore.asList())


}