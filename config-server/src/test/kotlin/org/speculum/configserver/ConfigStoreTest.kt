package org.speculum.configserver

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.speculum.config.MirrorConfig
import org.speculum.config.ModuleConfig
import java.nio.file.Files
import java.nio.file.Path

class ConfigStoreTest {

    @Test
    fun savesAndLoadsRoundTrip(@TempDir dir: Path) {
        val file = dir.resolve("config.json")
        System.setProperty("mirror.config", file.toString())
        try {
            val config = MirrorConfig(
                timeFormat = 12,
                modules = listOf(
                    ModuleConfig("clock", "top_left", 0, mapOf("displaySeconds" to "false")),
                    ModuleConfig("weather", "top_right", 600_000, mapOf("lat" to "52.5")),
                )
            )
            ConfigStore.save(config)
            assertTrue(Files.exists(file))
            assertEquals(config, ConfigStore.load())
        } finally {
            System.clearProperty("mirror.config")
        }
    }

    @Test
    fun loadStripsStaleCurrentVersion(@TempDir dir: Path) {
        val file = dir.resolve("config.json")
        System.setProperty("mirror.config", file.toString())
        try {
            file.toFile().writeText(
                """{"modules":[{"module":"updatenotifier","config":{"repo":"a/b","currentVersion":"dev"}}]}"""
            )
            val loaded = ConfigStore.load()
            assertEquals(mapOf("repo" to "a/b"), loaded.modules.single().config)
        } finally {
            System.clearProperty("mirror.config")
        }
    }

    @Test
    fun loadsDefaultWhenMissing(@TempDir dir: Path) {
        System.setProperty("mirror.config", dir.resolve("absent.json").toString())
        try {
            assertEquals(MirrorConfig(), ConfigStore.load())
        } finally {
            System.clearProperty("mirror.config")
        }
    }
}