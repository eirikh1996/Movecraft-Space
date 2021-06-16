package io.github.eirikh1996.movecraftspace.expansion.selection

import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

object SelectionManager : Listener {
    val selections = HashMap<UUID, Selection>()
    val selectionsDisabled = HashSet<UUID>()
    lateinit var pl : JavaPlugin
    lateinit var file : File

    fun initialize() {
        file = File(pl.dataFolder, "selectiondisabled")
        if (file.exists()) {
            val yaml = YamlConfiguration()
            yaml.load(file)
            yaml.getStringList("disabledWand").forEach { s -> selectionsDisabled.add(UUID.fromString(s)) }
        }
    }
    @EventHandler
    fun onWandUse(event : PlayerInteractEvent) {
        val player = event.player
        if (!player.hasPermission("movecraftspace.wand") ||
            event.item == null ||
            event.item!!.type != Settings.SelectionWand ||
            selectionsDisabled.contains(player.uniqueId))
            return
        event.isCancelled = true
        val action = event.action
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val clicked = event.clickedBlock?.location!!
        if (action == Action.LEFT_CLICK_BLOCK) {
            player.sendMessage(MSUtils.COMMAND_PREFIX + "Started selection at (" + clicked.blockX + ", " + clicked.blockY + ", " + clicked.blockZ + "). Right click with your selection wand to encompass selection")
            if (selections.containsKey(player.uniqueId)) {
                val selection = selections[player.uniqueId]!!
                selection.minX = clicked.blockX
                selection.maxX = clicked.blockX
                selection.maxY = clicked.blockY
                selection.minY = clicked.blockY
                selection.minZ = clicked.blockZ
                selection.maxZ = clicked.blockZ
                selections.put(player.uniqueId, selection)
                return
            }
            selections.put(player.uniqueId, Selection(clicked.blockX, clicked.blockX, clicked.blockY, clicked.blockY, clicked.blockZ, clicked.blockZ, clicked.world!!))
            return
        }
        val selection = selections[player.uniqueId]
        if (selection == null) {
            player.sendMessage(MSUtils.COMMAND_PREFIX + "There is no selection available. Start by left-click the block of the hyperdrive structure, then right click to encompass the structure")
            return
        }
        var selChanged = false
        if (selection.minX > clicked.blockX) {
            selChanged = true
            selection.minX = clicked.blockX
        }
        if (selection.maxX < clicked.blockX) {
            selChanged = true
            selection.maxX = clicked.blockX
        }
        if (selection.minY > clicked.blockY) {
            selChanged = true
            selection.minY = clicked.blockY
        }
        if (selection.maxY < clicked.blockY) {
            selChanged = true
            selection.maxY = clicked.blockY
        }
        if (selection.minZ > clicked.blockZ) {
            selChanged = true
            selection.minZ = clicked.blockZ
        }
        if (selection.maxZ < clicked.blockZ) {
            selChanged = true
            selection.maxZ = clicked.blockZ
        }
        if (selChanged) {
            player.sendMessage(MSUtils.COMMAND_PREFIX + "Encompassed selection to (" + clicked.blockX + ", " + clicked.blockY + ", " + clicked.blockZ + "). Size " + selection.size)
            selections.put(player.uniqueId, selection)
        }

    }

    fun saveDisableWandList() {
        if (!file.exists())
            file.createNewFile()
        val yaml = YamlConfiguration()
        val list = ArrayList<String>()
        selectionsDisabled.forEach { id -> list.add(id.toString()) }
        yaml.set("disabledWand", list)
    }
}