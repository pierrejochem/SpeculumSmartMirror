package org.speculum.configserver

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class AuthTest {

    private lateinit var dir: File

    // Isolate credential storage to a temp dir so tests never read or clobber a
    // real ~/.speculum/admin-auth.json (CredentialStore resolves its file next
    // to ConfigPaths.configFile()).
    @BeforeEach
    fun setUp() {
        dir = Files.createTempDirectory("speculum-auth-test").toFile()
        System.setProperty("mirror.config", File(dir, "config.json").absolutePath)
    }

    @AfterEach
    fun tearDown() {
        System.clearProperty("mirror.config")
        dir.deleteRecursively()
    }

    @Test
    fun issuedTokenIsValid() {
        assertTrue(Auth.isValid(Auth.issueToken()))
    }

    @Test
    fun rejectsBadTokens() {
        val good = Auth.issueToken()
        assertFalse(Auth.isValid(good + "x"))       // tampered signature
        assertFalse(Auth.isValid("garbage"))         // no separator
        assertFalse(Auth.isValid("999.deadbeef"))    // wrong signature
        assertFalse(Auth.isValid(null))
        assertFalse(Auth.isValid(""))
    }

    @Test
    fun rejectsExpiredToken() {
        // exp in the past, signed-looking but stale -> invalid regardless of sig.
        assertFalse(Auth.isValid("1.someSignature"))
    }

    @Test
    fun bootstrapPasswordWhenNoneStored() {
        // Default password is "admin" when MIRROR_ADMIN_PASSWORD is unset and no
        // credential file has been written yet.
        assertTrue(Auth.checkPassword("admin"))
        assertFalse(Auth.checkPassword("wrong"))
    }

    @Test
    fun changePasswordPersistsAndReplacesBootstrap() {
        assertTrue(Auth.changePassword("admin", "secret9"))
        assertTrue(CredentialStore.file.exists())
        assertTrue(Auth.checkPassword("secret9"))
        assertFalse(Auth.checkPassword("admin"))     // bootstrap no longer accepted
    }

    @Test
    fun changePasswordRejectsWrongCurrentAndShortNew() {
        assertFalse(Auth.changePassword("wrong", "secret9"))   // wrong current
        assertFalse(Auth.changePassword("admin", "ab"))         // too short
        assertFalse(CredentialStore.file.exists())              // nothing written
        assertTrue(Auth.checkPassword("admin"))                 // unchanged
    }
}