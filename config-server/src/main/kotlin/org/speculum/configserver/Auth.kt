package org.speculum.configserver

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Minimal password gate + signed bearer tokens (no external auth lib).
 *
 *  - Password: a salted PBKDF2 hash persisted by [CredentialStore] once changed
 *    via the admin UI. Until then it bootstraps from `MIRROR_ADMIN_PASSWORD`
 *    (default "admin").
 *  - Token: `"<expiryMillis>.<hmacSHA256(expiryMillis)>"`, signed with
 *    `MIRROR_ADMIN_SECRET` (default a built-in string — override in production).
 */
object Auth {
    private val bootstrapPassword = System.getenv("MIRROR_ADMIN_PASSWORD") ?: "admin"
    private val secret = (System.getenv("MIRROR_ADMIN_SECRET")
        ?: "magicmirror-change-this-secret").toByteArray()
    private const val TTL_MS = 12 * 60 * 60 * 1000L
    const val MIN_PASSWORD_LENGTH = 4

    fun checkPassword(candidate: String): Boolean {
        val cred = CredentialStore.load()
        return if (cred != null) CredentialStore.verify(candidate, cred)
        else constantTimeEquals(candidate, bootstrapPassword)
    }

    /**
     * Verifies [current] then persists [new] as the admin password. Returns
     * false if the current password is wrong or [new] is too short; the stored
     * credential is left untouched in that case.
     */
    fun changePassword(current: String, new: String): Boolean {
        if (new.length < MIN_PASSWORD_LENGTH) return false
        if (!checkPassword(current)) return false
        CredentialStore.store(new)
        return true
    }

    fun issueToken(): String {
        val exp = System.currentTimeMillis() + TTL_MS
        return "$exp.${sign(exp.toString())}"
    }

    fun isValid(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        val parts = token.split(".", limit = 2)
        if (parts.size != 2) return false
        val (expStr, sig) = parts
        val exp = expStr.toLongOrNull() ?: return false
        if (exp < System.currentTimeMillis()) return false
        return constantTimeEquals(sig, sign(expStr))
    }

    private fun sign(data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret, "HmacSHA256"))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.toByteArray()))
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var r = 0
        for (i in a.indices) r = r or (a[i].code xor b[i].code)
        return r == 0
    }
}