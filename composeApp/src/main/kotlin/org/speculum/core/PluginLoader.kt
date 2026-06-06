package org.speculum.core

import java.io.File
import java.net.URLClassLoader
import java.util.ServiceLoader

/**
 * Loads every `*.jar` in the `plugins/` folder into a child classloader
 * (parent = the app loader, so shared API + Compose classes resolve to the
 * same types), then finds [ModuleFactory] implementations via the JDK
 * [ServiceLoader] (reflection over `META-INF/services`).
 *
 * Looks first in the packaged app's bundled resources (when installed via the
 * `.deb`/jpackage image, `compose.application.resources.dir` points there),
 * then falls back to a `plugins/` folder in the working directory (dev runs).
 */
fun discoverPluginFactories(): List<ModuleFactory> {
    val dir = pluginsDir() ?: return emptyList()
    val jars = dir.listFiles { f -> f.isFile && f.extension == "jar" }?.toList().orEmpty()
    if (jars.isEmpty()) return emptyList()

    val loader = URLClassLoader(
        jars.map { it.toURI().toURL() }.toTypedArray(),
        ModuleFactory::class.java.classLoader
    )
    return runCatching {
        ServiceLoader.load(ModuleFactory::class.java, loader).toList()
    }.onFailure { println("[plugins] failed to load module JARs: $it") }
        .getOrDefault(emptyList())
}

private fun pluginsDir(): File? {
    System.getProperty("compose.application.resources.dir")
        ?.let { File(it, "plugins") }
        ?.takeIf { it.isDirectory }
        ?.let { return it }
    return File("plugins").takeIf { it.isDirectory }
}