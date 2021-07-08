package io.github.eirikh1996.movecraftspace

import io.github.eirikh1996.movecraftspace.commands.MovecraftSpaceCommand
import io.github.eirikh1996.movecraftspace.commands.PlanetCommand
import io.github.eirikh1996.movecraftspace.commands.StarCommand
import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.listener.BlockListener
import io.github.eirikh1996.movecraftspace.listener.ExplosionListener
import io.github.eirikh1996.movecraftspace.listener.MovecraftListener
import io.github.eirikh1996.movecraftspace.listener.PlayerListener
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.objects.StarCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import net.countercraft.movecraft.WorldHandler
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.lang.Exception
import java.lang.System.currentTimeMillis
import java.lang.reflect.Method

class MovecraftSpace : JavaPlugin() {
    private var ticks = 0
    private var tickTimeStamp = 0L

    companion object {
        lateinit var instance : MovecraftSpace
    }

    override fun onEnable() {
        MSUtils.displayTitle()
        saveDefaultConfig()
        ConfigHolder.config = config
        val packageName = server.javaClass.`package`.name
        val ver = packageName.substring(packageName.lastIndexOf(".") + 1).split("_")[1].toInt()
        Settings.IsLegacy = ver <= 12
        Settings.IsV1_17 = ver >= 17
        Settings.IsV1_13 = ver >= 13



        PlanetCollection.pl = this
        PlanetCollection.loadPlanets()
        StarCollection.pl = this
        StarCollection.loadStars()
        server.pluginManager.registerEvents(MovecraftListener, this)
        server.pluginManager.registerEvents(PlayerListener, this)
        server.pluginManager.registerEvents(ExplosionListener, this)
        server.pluginManager.registerEvents(BlockListener, this)
        getCommand("planet")!!.setExecutor(PlanetCommand)
        getCommand("movecraftspace")!!.setExecutor(MovecraftSpaceCommand)
        getCommand("star")!!.setExecutor(StarCommand)
        if (Settings.RotatePlanets)
            PlanetaryMotionManager.runTaskTimerAsynchronously(this, 100, 20)
        tickTimeStamp = currentTimeMillis()
        object : BukkitRunnable() {
            override fun run() {
                //Reset tick counter if it reaches max value, to avoid overflows
                if (ticks >= Int.MAX_VALUE) {
                    ticks = 0
                    tickTimeStamp = currentTimeMillis()
                }
                ticks++
            }

        }.runTaskTimer(this, 0, 1)
        object : BukkitRunnable() {
            override fun run() {
                ExpansionManager.enableExpansions()
            }
        }.runTaskLater(this, 10)
        server.pluginManager.registerEvents(UpdateManager, this)
    }

    fun averageTicks() : Double {
        val tps = ticks / ((currentTimeMillis() - tickTimeStamp) / 1000.0)
        tickTimeStamp = currentTimeMillis()
        ticks = 0
        return tps
    }

    override fun onLoad() {
        ExpansionManager.pl = this
        instance = this
        ExpansionManager.loadExpansions()
    }

    override fun onDisable() {
        ExpansionManager.disableExpansions()
        server.scheduler.cancelTasks(this)
    }
}