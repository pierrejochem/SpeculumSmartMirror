package org.speculum.config

import java.io.File

/**
 * Resolves the single config file shared by the mirror app and the config
 * server. Order: `-Dmirror.config=…` JVM property, then `MIRROR_CONFIG` env,
 * then `<user.home>/.magicmirror/config.json` (writable even when the app is
 * installed read-only under /opt).
 */
object ConfigPaths {
    fun configFile(): File {
        (System.getProperty("mirror.config") ?: System.getenv("MIRROR_CONFIG"))
            ?.let { return File(it) }
        val dir = File(System.getProperty("user.home"), ".speculum")
        runCatching { dir.mkdirs() }
        return File(dir, "config.json")
    }
}