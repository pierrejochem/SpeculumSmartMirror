package org.speculum.configserver

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.speculum.config.ConfigPaths
import org.speculum.config.MirrorConfig
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Reads/writes the persisted mirror configuration. Same file the desktop app
 * loads at boot (`MIRROR_CONFIG` env var, else `config.json` in the working dir).
 */
object ConfigStore {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val file: File get() = ConfigPaths.configFile()

    fun load(): MirrorConfig =
        if (file.exists()) runCatching { json.decodeFromString<MirrorConfig>(file.readText()) }
            .getOrDefault(MirrorConfig())
        else MirrorConfig()

    fun save(config: MirrorConfig) {
        // Write to a temp file then atomically rename, so a file-watcher (the
        // mirror's hot-reload) never observes a half-written config.
        val tmp = File(file.absoluteFile.parentFile, "${file.name}.tmp")
        tmp.writeText(json.encodeToString(config))
        runCatching {
            Files.move(
                tmp.toPath(), file.toPath(),
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE
            )
        }.onFailure { tmp.copyTo(file, overwrite = true); tmp.delete() }
    }
}