package org.speculum.configserver

import kotlinx.serialization.Serializable
import org.speculum.config.ModuleConfig
import org.speculum.core.ModuleFactory
import java.io.File
import java.net.URLClassLoader
import java.util.ServiceLoader

/** A module the UI can add, with its suggested default placement/options. */
@Serializable
data class AvailableModule(
    val name: String,
    val order: Int,
    val defaultConfig: ModuleConfig?,
)

/**
 * Discovers installable modules by scanning the `plugins/` folder for JARs and
 * loading their [ModuleFactory] services — same mechanism the app uses. Lets
 * the admin UI offer the real set of modules with sensible defaults.
 */
fun scanAvailableModules(): List<AvailableModule> {
    val dir = pluginsDir() ?: return emptyList()
    val jars = dir.listFiles { f -> f.isFile && f.extension == "jar" }?.toList().orEmpty()
    if (jars.isEmpty()) return emptyList()

    val loader = URLClassLoader(
        jars.map { it.toURI().toURL() }.toTypedArray(),
        ModuleFactory::class.java.classLoader
    )
    return runCatching {
        ServiceLoader.load(ModuleFactory::class.java, loader)
            .map { AvailableModule(it.name, it.order, it.defaultConfig()) }
            .sortedBy { it.order }
    }.getOrDefault(emptyList())
}

private fun pluginsDir(): File? {
    System.getenv("MIRROR_PLUGINS")?.let { File(it).takeIf { d -> d.isDirectory }?.let { d -> return d } }
    System.getProperty("compose.application.resources.dir")
        ?.let { File(it, "plugins") }?.takeIf { it.isDirectory }?.let { return it }
    return File("plugins").takeIf { it.isDirectory }
}