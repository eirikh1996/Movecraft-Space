package io.github.eirikh1996.movecraftspace.utils

import io.github.eirikh1996.movecraftspace.Settings
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object InventoryUtils {



    fun createItem(type : Material, data : Byte = 0, displayName : String, lore : List<String> = emptyList()) : ItemStack {
        val item = if (Settings.IsLegacy) ItemStack(type, 1, 0, data) else ItemStack(type)
        val meta = item.itemMeta!!
        meta.setDisplayName(displayName)
        meta.lore = lore
        return item
    }

    fun createItem(type : Material, data : Byte = 0, displayName : String, vararg lore : String = emptyArray()) = createItem(type, data, displayName, lore.asList())

    fun createItem(type: Material, displayName: String, vararg lore : String = emptyArray()) = createItem(type, 0, displayName, lore.asList())


}