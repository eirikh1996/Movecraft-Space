package io.github.eirikh1996.movecraftspace.expansion

import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.URL
import java.net.URLClassLoader
import java.util.logging.Logger


class ExpansionClassLoader constructor(parent : ClassLoader, val desc : YamlConfiguration, val datafolder : File, jar : File, val plugin: Plugin) : URLClassLoader(arrayOf(jar.toURI().toURL()), parent) {
    val expansion : Expansion

    init {
        val className = desc.getString("main")
        val jarClass = Class.forName(className, true, this)
        val expansionClass = jarClass.asSubclass(Expansion::class.java)
        Bukkit.getLogger().info(expansionClass.toString())
        expansion = expansionClass.getConstructor().newInstance() as Expansion
    }

    override fun findClass(name: String?): Class<*> {
        return super.findClass(name)
    }
}