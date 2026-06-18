package org.speculum.config

import kotlinx.serialization.json.Json

/**
 * Loads the mirror configuration. Prefers the persisted `config.json` (managed
 * by the web admin in `:config-server`; path from [ConfigPaths]), and falls
 * back to a minimal default. Modules not listed are still added automatically
 * from their plugin `defaultConfig()` at boot.
 */
object ConfigLoader {
    private val json = Json { ignoreUnknownKeys = true }

    /** Last-modified time of the config file (0 if absent) — used to detect edits. */
    fun lastModified(): Long = ConfigPaths.configFile().let { if (it.exists()) it.lastModified() else 0L }

    fun load(): MirrorConfig {
        val file = ConfigPaths.configFile()
        if (file.exists()) {
            runCatching { json.decodeFromString<MirrorConfig>(file.readText()) }
                .onSuccess { return it.sanitized() }
        }
        return json.decodeFromString(DEFAULT_CONFIG)
    }

    private val DEFAULT_CONFIG = """
    {
      "language": "en",
      "timeFormat": 24,
      "units": "metric",
      "modules": []
    }
    """.trimIndent()
}
