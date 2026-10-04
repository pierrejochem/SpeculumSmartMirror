package org.speculum.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * How one setting should be presented and validated. The admin console has no
 * built-in knowledge of any module's options: it renders whatever a module
 * declares through [org.speculum.core.ModuleFactory.settingsSchema].
 */
@Serializable
enum class SettingType {
    /** Single-line free text. */
    @SerialName("string") STRING,

    /** Whole number, optionally bounded by [SettingSpec.min]/[SettingSpec.max]. */
    @SerialName("int") INT,

    /** `"true"`/`"false"`, rendered as a checkbox. */
    @SerialName("bool") BOOL,

    /** One of [SettingSpec.options], rendered as a dropdown. */
    @SerialName("enum") ENUM,

    /** Multi-line free text, rendered as a textarea. */
    @SerialName("text") TEXT,

    /** Absolute URL; same control as [STRING] with URL-shaped validation. */
    @SerialName("url") URL,

    /** A host IPv4 address, offered from the addresses the server detects. */
    @SerialName("ip") IP,

    /**
     * Handled by a purpose-built editor the console looks up by
     * [SettingSpec.editor]. Falls back to [TEXT] when no editor matches, so an
     * unknown editor name degrades instead of hiding the value.
     */
    @SerialName("custom") CUSTOM,
}

/**
 * Declares one entry of a module's `config` map so the admin console can render
 * a labelled, typed control instead of a raw key/value text row.
 *
 * A module returns these from [org.speculum.core.ModuleFactory.settingsSchema].
 * Keys present in a saved config but absent from the schema still render as raw
 * key/value rows, so a schema may describe only part of a module's options and
 * a module may ship none at all.
 *
 * @property key the `config` map key this describes.
 * @property label short human-readable name shown next to the control.
 * @property type which control to render; see [SettingType].
 * @property default value the module itself falls back to, shown as a hint when
 *   the key is unset. Keep it equal to the default the module code reads.
 * @property options allowed values for [SettingType.ENUM]; ignored otherwise.
 * @property min lower bound for [SettingType.INT]; null leaves it unbounded.
 * @property max upper bound for [SettingType.INT]; null leaves it unbounded.
 * @property step increment for [SettingType.INT] spinners.
 * @property help one sentence explaining the effect of the setting.
 * @property placeholder shown in an empty text field.
 * @property editor name of the dedicated editor for [SettingType.CUSTOM].
 * @property preview templates for a live "effective value" hint, tried in order.
 *   A template interpolates `{key}` from the module's current config and
 *   supports `{key|fallback}`. The first template whose plain `{key}`
 *   references all resolve to non-blank values is shown.
 * @property advanced true to tuck the control behind a "show advanced" toggle.
 */
@Serializable
data class SettingSpec(
    val key: String,
    val label: String,
    val type: SettingType = SettingType.STRING,
    val default: String = "",
    val options: List<String> = emptyList(),
    val min: Int? = null,
    val max: Int? = null,
    val step: Int? = null,
    val help: String = "",
    val placeholder: String = "",
    val editor: String = "",
    val preview: List<String> = emptyList(),
    val advanced: Boolean = false,
)

/** The declared defaults, as a `config` map. */
fun List<SettingSpec>.defaults(): Map<String, String> =
    filter { it.default.isNotEmpty() }.associate { it.key to it.default }

/**
 * Keys of [config] this schema does not describe, sorted. The admin console
 * renders these as raw key/value rows; a module's own `defaultConfig()` should
 * normally leave none behind.
 */
fun List<SettingSpec>.undeclaredKeys(config: Map<String, String>): List<String> {
    val declared = mapTo(HashSet()) { it.key }
    return config.keys.filterNot { it in declared }.sorted()
}
