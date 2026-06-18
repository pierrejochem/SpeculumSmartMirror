package org.speculum.update

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VersionTest {

    @Test
    fun newerComparesNumerically() {
        assertTrue(isNewer("1.2.0", "1.1.9"))
        assertTrue(isNewer("v0.4.3", "0.4.2"))
        assertTrue(isNewer("0.5.0", "0.4.9"))
        assertFalse(isNewer("0.4.2", "0.4.2"))
        assertFalse(isNewer("0.4.1", "0.4.2"))
    }

    @Test
    fun preReleaseSuffixDropped() {
        assertFalse(isNewer("1.0.0-rc1", "1.0.0"))
        assertTrue(isNewer("1.0.1-rc1", "1.0.0"))
    }

    @Test
    fun devVersionDetection() {
        assertTrue(isDevVersion("dev"))
        assertTrue(isDevVersion(""))
        assertTrue(isDevVersion("DEV"))
        assertFalse(isDevVersion("0.4.2"))
        assertFalse(isDevVersion("1.0.0-rc1"))
    }
}
