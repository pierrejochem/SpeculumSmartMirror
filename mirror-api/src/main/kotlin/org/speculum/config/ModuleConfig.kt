package org.speculum.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.speculum.core.Region

/**
 * Equivalent to one entry in MagicMirror's config.js `modules: [...]` array.
 * `config` is a free-form map so each module reads its own options.
 */
@Serializable
data class ModuleConfig(
    val module: String,
    val position: String = "top_left",
    @SerialName("refreshInterval") val refreshIntervalMs: Long = 60_000,
    val config: Map<String, String> = emptyMap()
) {
    val regionEnum: Region
        get() = runCatching { Region.valueOf(position.uppercase()) }
            .getOrDefault(Region.TOP_LEFT)

    fun string(key: String, default: String = ""): String = config[key] ?: default
    fun int(key: String, default: Int = 0): Int = config[key]?.toIntOrNull() ?: default
    fun bool(key: String, default: Boolean = false): Boolean =
        config[key]?.toBooleanStrictOrNull() ?: default
}

/** Top-level config, equivalent to MagicMirror's config.js. */
@Serializable
data class MirrorConfig(
    val language: String = "en",
    val timeFormat: Int = 24,
    val units: String = "metric",
    val modules: List<ModuleConfig> = emptyList()
)