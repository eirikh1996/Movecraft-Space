package io.github.eirikh1996.movecraftspace

import com.google.gson.Gson
import io.github.eirikh1996.movecraftspace.utils.MSUtils.COMMAND_PREFIX
import io.github.eirikh1996.movecraftspace.utils.MSUtils.WARNING
import org.bukkit.Bukkit
import org.bukkit.Bukkit.getConsoleSender
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.scheduler.BukkitRunnable
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

object UpdateManager : BukkitRunnable(), Listener {
    var update = ""

    init {
        runTaskTimerAsynchronously(MovecraftSpace.instance, 0, 100000)
    }

    override fun run() {
        val newVersion = newUpdateAvailable()
        getConsoleSender().sendMessage(COMMAND_PREFIX + "Checking for updates")
        object : BukkitRunnable() {
            override fun run() {
                if (newVersion != null) {
                    for (p in Bukkit.getOnlinePlayers()) {
                        if (!p.hasPermission("movecraftspace.update")) {
                            continue
                        }
                        p.sendMessage(COMMAND_PREFIX + "An update of MovecraftSpace is now available. Download from https://dev.bukkit.org/projects/Movecraft-Space")
                    }
                    getConsoleSender().sendMessage(COMMAND_PREFIX + WARNING + "Update " + update + " of MovecraftSpace is available. You're still on " + MovecraftSpace.instance.description.version)
                    update = newVersion
                    return
                }
                getConsoleSender().sendMessage(COMMAND_PREFIX + "You are up to date")
            }
        }.runTaskLaterAsynchronously(MovecraftSpace.instance, 100)
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        if (!event.player.hasPermission("movecraftspace.update"))
            return
        object : BukkitRunnable() {
            override fun run() {
                val newVer: String = newUpdateAvailable() ?: return
                event.player
                    .sendMessage(COMMAND_PREFIX + "An update of MovecraftSpace is now available. Download from https://dev.bukkit.org/projects/MovecraftSpace")
            }
        }.runTaskLaterAsynchronously(MovecraftSpace.instance, 60)
    }

    fun newUpdateAvailable(): String? {
        try {
            val url = URL("https://servermods.forgesvc.net/servermods/files?projectids=408014")
            val conn = url.openConnection()
            conn.readTimeout = 5000
            conn.addRequestProperty("User-Agent", "MovecraftSpace Update Checker")
            conn.doOutput = true
            val reader = BufferedReader(InputStreamReader(conn.getInputStream()))
            val response = reader.readLine()
            val gson = Gson()
            val objList =
                gson.fromJson<List<*>>(response, MutableList::class.java)
            if (objList.size == 0) {
                MovecraftSpace.instance.getLogger().warning("No files found, or Feed URL is bad.")
                return null
            }
            val iter = objList.iterator()
            val foundVersions = ArrayList<String>()
            while (iter.hasNext()) {
                val data = iter.next() as Map<String, Any>
                val versionName = data["name"] as String
                if (!versionName.startsWith("Movecraft-Space", true))
                    continue
                foundVersions.add(versionName)
            }
            val versionName = foundVersions[foundVersions.size - 1]!!
            val currVersion = MovecraftSpace.instance.getDescription().getVersion().replace("v", "")
            val newVersion = versionName!!.substring(versionName.lastIndexOf("v") + 1)
            val cv = (currVersion.split(".")[0].toInt() * 1000) + currVersion.split(".")[1].toInt()
            val nv = (newVersion.split(".")[0].toInt() * 1000) + newVersion.split(".")[1].toInt()
            if (nv > cv)
                return newVersion
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

}