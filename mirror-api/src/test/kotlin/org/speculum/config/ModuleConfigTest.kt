package org.speculum.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.speculum.core.Region

class ModuleConfigTest {

    @Test
    fun defaults() {
        val c = ModuleConfig("clock")
        assertEquals("top_left", c.position)
        assertEquals(60_000, c.refreshIntervalMs)
        assertEquals(Region.TOP_LEFT, c.regionEnum)
    }

    @Test
    fun typedGetters() {
        val c = ModuleConfig("x", config = mapOf("s" to "hi", "i" to "7", "b" to "true"))
        assertEquals("hi", c.string("s"))
        assertEquals("fallback", c.string("missing", "fallback"))
        assertEquals(7, c.int("i"))
        assertEquals(3, c.int("missing", 3))
        assertEquals(true, c.bool("b"))
        assertEquals(false, c.bool("missing"))
        assertEquals(0, c.int("s")) // non-numeric -> default
    }

    @Test
    fun regionMapping() {
        assertEquals(Region.BOTTOM_BAR, ModuleConfig("x", position = "bottom_bar").regionEnum)
        assertEquals(Region.LOWER_THIRD, ModuleConfig("x", position = "LOWER_THIRD").regionEnum)
        assertEquals(Region.TOP_LEFT, ModuleConfig("x", position = "nonsense").regionEnum)
    }
}