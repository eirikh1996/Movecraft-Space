package io.github.eirikh1996.movecraftspace.expansion

import com.google.common.io.ByteStreams
import io.github.eirikh1996.movecraftspace.expansion.ExpansionManager.classCache
import io.github.eirikh1996.movecraftspace.expansion.exception.InvalidExpansionException
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.IOException
import java.lang.ClassCastException
import java.lang.Exception
import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader
import java.security.CodeSource
import java.util.jar.JarFile


class ExpansionClassLoader constructor(parent : ClassLoader, val desc : YamlConfiguration, val datafolder : File, jar : File, val plugin: JavaPlugin) : URLClassLoader(arrayOf(jar.toURI().toURL()), parent) {
    val expansion : Expansion
    val jar = JarFile(jar)
    val url = jar.toURI().toURL()
    val pluginFindClass : Method

    init {
        pluginFindClass = plugin.javaClass.classLoader.javaClass.getDeclaredMethod("findClass", String::class.java)
        pluginFindClass.isAccessible = true
        val className = desc.getString("main")
        if (className == null)
            throw InvalidExpansionException("main class name cannot be null in expansion.yml")
        val jarClass = Class.forName(className, true, this)
        val expansionClass : Class<out Expansion>
        try {
           expansionClass = jarClass.asSubclass(Expansion::class.java)
        } catch (e : ClassCastException) {
            throw InvalidExpansionException("Main class " + className + " does not extend class io.github.eirikh1996.movecraftspace.expansion.Expansion", e)
        }

        classCache.put(expansionClass.name, expansionClass)
        expansion = expansionClass.getConstructor().newInstance() as Expansion
    }

    override fun findClass(name: String): Class<*> {
        var path = name
        var result = classCache[name]
        if (result != null) {
            return result
        }
        path = path.replace(".", "/")
        path += ".class"
        val entry = jar.getJarEntry(path)
        if (entry != null) {
            val classBytes : ByteArray
            try {
                var throwable : Throwable? = null
                try {
                    val input = jar.getInputStream(entry)
                    try {
                        classBytes = ByteStreams.toByteArray(input)
                    } finally {
                        if (input != null)
                            input.close()
                    }
                } catch (t : Throwable) {
                    if (throwable == null)
                        throwable = t
                    else if (throwable != t) {
                        throwable.addSuppressed(t)
                    }
                    throw throwable
                }
            } catch (e : IOException) {
                throw ClassNotFoundException(name, e)
            }
            val dot = name.lastIndexOf(".")
            val signers = entry.codeSigners
            val source = CodeSource(url, signers)
            result = defineClass(name, classBytes, 0, classBytes.size, source)
        }
        if (result == null) {
            result = Class.forName(path.replace("/", "."))
        }
        if (result == null) {
            return super.findClass(name)
        }
        classCache.put(name, result)
        return result
    }

    override fun getResource(name: String?): URL? {
        return super.findResource(name)
    }
}