package org.speculum.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.speculum.config.ModuleConfig

class ModuleFactoryTest {

    /** A plugin written before the schema existed: it overrides nothing extra. */
    private class LegacyFactory : ModuleFactory {
        override val name = "legacy"
        override fun create(config: ModuleConfig): MirrorModule = error("not needed")
    }

    @Test
    fun `a factory that declares no schema keeps compiling and reports none`() {
        assertEquals(emptyList<Any>(), LegacyFactory().settingsSchema())
    }
}
