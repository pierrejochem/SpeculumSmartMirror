package org.speculum.modules.weather

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.speculum.config.SettingType
import org.speculum.config.undeclaredKeys

/**
 * Guards the schema against the module's own defaults drifting apart: every key
 * the factory suggests must have a [org.speculum.config.SettingSpec], or the
 * admin console falls back to a raw key/value row for it.
 */
class WeatherSchemaTest {

    private val factories = listOf(WeatherModuleFactory(), WeatherForecastModuleFactory())

    @Test
    fun `schema declares every key the default config ships`() {
        for (f in factories) {
            val cfg = f.defaultConfig()?.config.orEmpty()
            assertEquals(
                emptyList<String>(),
                f.settingsSchema().undeclaredKeys(cfg),
                "${f.name}: defaultConfig keys missing from settingsSchema",
            )
        }
    }

    @Test
    fun `schema entries are well formed`() {
        for (f in factories) {
            val schema = f.settingsSchema()
            assertTrue(schema.isNotEmpty(), "${f.name}: declares no schema")
            assertEquals(
                schema.map { it.key }.distinct(),
                schema.map { it.key },
                "${f.name}: duplicate keys in settingsSchema",
            )
            for (s in schema) {
                assertTrue(s.key.isNotBlank(), "${f.name}: a spec has a blank key")
                assertTrue(s.label.isNotBlank(), "${f.name}/${s.key}: blank label")
                if (s.type == SettingType.ENUM) {
                    assertTrue(s.options.isNotEmpty(), "${f.name}/${s.key}: enum without options")
                    assertTrue(
                        s.default.isEmpty() || s.default in s.options,
                        "${f.name}/${s.key}: default '${s.default}' not among options ${s.options}",
                    )
                }
                if (s.type == SettingType.CUSTOM) {
                    assertTrue(s.editor.isNotBlank(), "${f.name}/${s.key}: custom type without an editor name")
                }
                if (s.type == SettingType.INT && s.default.isNotEmpty()) {
                    val n = s.default.toIntOrNull()
                    assertTrue(n != null, "${f.name}/${s.key}: int default '${s.default}' is not a number")
                    if (s.min != null) assertTrue(n!! >= s.min!!, "${f.name}/${s.key}: default below min")
                    if (s.max != null) assertTrue(n!! <= s.max!!, "${f.name}/${s.key}: default above max")
                }
                if (s.min != null && s.max != null) {
                    assertTrue(s.min!! <= s.max!!, "${f.name}/${s.key}: min above max")
                }
            }
        }
    }
}
