package org.speculum.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConfigPathsTest {

    @Test
    fun honorsSystemProperty() {
        System.setProperty("mirror.config", "/tmp/mm-test-config.json")
        try {
            assertEquals("/tmp/mm-test-config.json", ConfigPaths.configFile().path)
        } finally {
            System.clearProperty("mirror.config")
        }
    }

    @Test
    fun defaultsUnderHome() {
        System.clearProperty("mirror.config")
        val f = ConfigPaths.configFile()
        assertEquals("config.json", f.name)
        assertEquals(".speculum", f.parentFile.name)
    }
}