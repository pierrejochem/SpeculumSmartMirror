package org.speculum.config

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettingSpecTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `serializes type as the lowercase wire name`() {
        val spec = SettingSpec(key = "units", label = "Units", type = SettingType.ENUM, options = listOf("metric"))
        val encoded = json.encodeToString(SettingSpec.serializer(), spec)
        assertTrue(encoded.contains("\"type\":\"enum\""), encoded)
    }

    @Test
    fun `omitted fields fall back so a minimal spec round-trips`() {
        val spec = json.decodeFromString(
            SettingSpec.serializer(),
            """{"key":"header","label":"Header"}"""
        )
        assertEquals(SettingType.STRING, spec.type)
        assertEquals("", spec.default)
        assertEquals(emptyList<String>(), spec.options)
        assertEquals(null, spec.min)
        assertEquals(null, spec.max)
        assertEquals(false, spec.advanced)
    }

    @Test
    fun `defaults collects only the specs that declare one`() {
        val schema = listOf(
            SettingSpec("a", "A", default = "1"),
            SettingSpec("b", "B"),
            SettingSpec("c", "C", default = "3"),
        )
        assertEquals(mapOf("a" to "1", "c" to "3"), schema.defaults())
    }

    @Test
    fun `undeclaredKeys reports config keys the schema does not describe`() {
        val schema = listOf(SettingSpec("known", "Known"))
        assertEquals(
            listOf("another", "stray"),
            schema.undeclaredKeys(mapOf("known" to "x", "stray" to "y", "another" to "z"))
        )
    }

    @Test
    fun `undeclaredKeys is empty when the schema covers the config`() {
        val schema = listOf(SettingSpec("a", "A"), SettingSpec("b", "B"))
        assertEquals(emptyList<String>(), schema.undeclaredKeys(mapOf("a" to "1", "b" to "2")))
    }

    @Test
    fun `an empty schema declares nothing`() {
        val schema = emptyList<SettingSpec>()
        assertEquals(emptyMap<String, String>(), schema.defaults())
        assertEquals(listOf("a"), schema.undeclaredKeys(mapOf("a" to "1")))
    }
}
