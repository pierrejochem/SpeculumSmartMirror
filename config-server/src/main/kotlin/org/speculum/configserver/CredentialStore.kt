package org.speculum.configserver

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.speculum.config.ConfigPaths
import java.io.File
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Persisted admin credential: PBKDF2-HMAC-SHA256 over a random salt. */
@Serializable
data class Credential(val salt: String, val hash: String, val iterations: Int)

/**
 * Stores the admin password as a salted PBKDF2 hash in `admin-auth.json`, next
 * to the shared config file (`~/.speculum/` by default). The plaintext password
 * is never written. Absent file ⇒ no password set yet, so [Auth] falls back to
 * the bootstrap env/default password until the first change.
 */
object CredentialStore {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private const val ITERATIONS = 120_000
    private const val KEY_LEN_BITS = 256
    private val rng = SecureRandom()

    val file: File get() = File(ConfigPaths.configFile().absoluteFile.parentFile, "admin-auth.json")

    fun load(): Credential? =
        if (file.exists()) runCatching { json.decodeFromString<Credential>(file.readText()) }.getOrNull()
        else null

    /** Hash [password] with a fresh salt and persist it (atomic-ish replace). */
    fun store(password: String) {
        val salt = ByteArray(16).also(rng::nextBytes)
        val cred = Credential(b64(salt), b64(pbkdf2(password, salt, ITERATIONS)), ITERATIONS)
        val parent = file.absoluteFile.parentFile
        runCatching { parent.mkdirs() }
        val tmp = File(parent, "${file.name}.tmp")
        tmp.writeText(json.encodeToString(cred))
        tmp.copyTo(file, overwrite = true)
        tmp.delete()
    }

    fun verify(password: String, cred: Credential): Boolean {
        val salt = runCatching { Base64.getDecoder().decode(cred.salt) }.getOrNull() ?: return false
        return constantTimeEquals(b64(pbkdf2(password, salt, cred.iterations)), cred.hash)
    }

    private fun pbkdf2(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LEN_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun b64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var r = 0
        for (i in a.indices) r = r or (a[i].code xor b[i].code)
        return r == 0
    }
}