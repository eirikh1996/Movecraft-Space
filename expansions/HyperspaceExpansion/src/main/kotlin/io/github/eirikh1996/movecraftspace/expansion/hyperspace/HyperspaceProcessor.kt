package io.github.eirikh1996.movecraftspace.expansion.hyperspace

import io.github.eirikh1996.movecraftspace.ConfigHolder
import io.github.eirikh1996.movecraftspace.Settings
import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.events.HyperspaceChargeEvent
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.events.HyperspaceEnterEvent
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.events.HyperspaceExitEvent
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.events.HyperspaceTravelEvent
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.managers.*
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.HyperspaceBeacon
import io.github.eirikh1996.movecraftspace.expansion.hyperspace.objects.HyperspaceTravelEntry
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.objects.StarCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import net.countercraft.movecraft.craft.Craft
import net.countercraft.movecraft.craft.CraftManager
import net.countercraft.movecraft.craft.PilotedCraft
import net.countercraft.movecraft.util.MathUtils
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.Sign
import org.bukkit.block.data.type.WallSign
import org.bukkit.boss.BarColor
import org.bukkit.boss.BossBar
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import java.lang.UnsupportedOperationException
import java.math.RoundingMode
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.*
import kotlin.random.Random

class HyperspaceProcessor (plugin: Plugin) : HyperspaceManager.HyperspaceProcessor<Craft>() {



}