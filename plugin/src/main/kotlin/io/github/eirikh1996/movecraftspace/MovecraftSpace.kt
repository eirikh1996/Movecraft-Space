package io.github.eirikh1996.movecraftspace

import io.github.eirikh1996.Movecraft8Handler
import io.github.eirikh1996.listener.Movecraft8Listener
import io.github.eirikh1996.movecraftspace.commands.MovecraftSpaceCommand
import io.github.eirikh1996.movecraftspace.commands.PlanetCommand
import io.github.eirikh1996.movecraftspace.commands.StarCommand
import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager
import io.github.eirikh1996.movecraftspace.listener.BlockListener
import io.github.eirikh1996.movecraftspace.listener.ExplosionListener
import io.github.eirikh1996.movecraftspace.listener.Movecraft7Listener
import io.github.eirikh1996.movecraftspace.listener.PlayerListener
import io.github.eirikh1996.movecraftspace.objects.PlanetCollection
import io.github.eirikh1996.movecraftspace.objects.StarCollection
import io.github.eirikh1996.movecraftspace.utils.MSUtils
import io.github.eirikh1996.movecraftspace.utils.MovecraftHandler
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.lang.System.currentTimeMillis

class MovecraftSpace : JavaPlugin() {
    private var ticks = 0
    private var tickTimeStamp = 0L
    internal lateinit var imageDir : File
    lateinit var movecraftHandler : MovecraftHandler<*>

    companion object {
        lateinit var instance : MovecraftSpace
    }

    override fun onEnable() {
        MSUtils.displayTitle()
        saveDefaultConfig()
        imageDir = File(dataFolder, "images")
        if (!imageDir.exists())
            imageDir.mkdirs()
        ConfigHolder.config = config
        val packageName = server.javaClass.`package`.name
        val ver = packageName.substring(packageName.lastIndexOf(".") + 1).split("_")[1].toInt()
        try {
            Class.forName("net.countercraft.movecraft.craft.BaseCraft")
            Settings.IsMovecraft8 = true
        } catch ( e : ClassNotFoundException) {
            Settings.IsMovecraft8 = false
        }
        if (Settings.IsMovecraft8)
            movecraftHandler = Movecraft8Handler()
        else
            movecraftHandler = Movecraft7Handler()
        Settings.IsLegacy = ver <= 12
        Settings.IsV1_17 = ver >= 17
        Settings.IsV1_13 = ver >= 13



        PlanetCollection.pl = this
        PlanetCollection.loadPlanets()
        StarCollection.pl = this
        StarCollection.loadStars()
        server.pluginManager.registerEvents(if (Settings.IsMovecraft8) Movecraft8Listener else Movecraft7Listener, this)
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
                server.pluginManager.registerEvents(UpdateManager, instance)
            }
        }.runTaskLater(this, 10)
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
        MSUtils.plugin = this
    }

    override fun onDisable() {
        ExpansionManager.disableExpansions()
        server.scheduler.cancelTasks(this)
    }
}