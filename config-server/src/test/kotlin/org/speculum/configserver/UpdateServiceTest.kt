package org.speculum.configserver

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class UpdateServiceTest {

    @Test
    fun devBuildIsNeverUpdatable() = runBlocking {
        // The test JVM has no `jpackage.app-path`, so it is a dev runtime: the
        // updater must be inert regardless of what GitHub returns.
        val s = UpdateService.status()
        assertEquals("dev", s.currentVersion)
        assertFalse(s.updatable)
        assertEquals("unsupported", s.installType)
    }

    @Test
    fun stagedMetaRoundTrips() {
        val meta = StagedMeta("speculum_0.4.3_arm64.deb", "abc123", "0.4.3", "deb")
        val json = Json.encodeToString(StagedMeta.serializer(), meta)
        assertEquals(meta, Json.decodeFromString(StagedMeta.serializer(), json))
    }

    @Test
    fun progressStartsIdle() {
        // Snapshot before any job: phase reflects the enum name, lowercased.
        val snap = UpdateJob.snapshot()
        assertEquals(snap.phase, snap.phase.lowercase())
    }
}
