package org.speculum.update

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.PGPSignatureList
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory
import org.bouncycastle.openpgp.jcajce.JcaPGPPublicKeyRingCollection
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider
import java.io.File
import java.security.MessageDigest

/** Outcome of verifying a downloaded package against a signed checksum file. */
sealed interface VerifyResult {
    data object Ok : VerifyResult
    data class Failed(val reason: String) : VerifyResult
}

/**
 * Verifies a downloaded release package — fail-closed. Requires BOTH a valid
 * OpenPGP signature over `SHA256SUMS` (against a pinned public key) AND a
 * matching SHA-256 of the package. A checksum without a signature is worthless
 * against a compromised release host, so a missing/empty signature is a refusal,
 * never a downgrade to checksum-only.
 */
object SignatureVerifier {

    private val provider = BouncyCastleProvider()

    /**
     * @param sumsText   contents of `SHA256SUMS`
     * @param sigArmored contents of `SHA256SUMS.asc` (ASCII-armored detached sig)
     * @param pubKey     the pinned public key (ASCII-armored)
     * @param pkg        the downloaded package file
     * @param assetName  the package's release-asset name (its key in SHA256SUMS)
     */
    fun verify(
        sumsText: String,
        sigArmored: ByteArray?,
        pubKey: ByteArray,
        pkg: File,
        assetName: String,
    ): VerifyResult {
        if (sigArmored == null || sigArmored.isEmpty())
            return VerifyResult.Failed("release is unsigned (no SHA256SUMS.asc)")

        val sigOk = runCatching { verifyDetached(sumsText.toByteArray(), sigArmored, pubKey) }
            .getOrDefault(false)
        if (!sigOk) return VerifyResult.Failed("SHA256SUMS signature is invalid")

        val expected = expectedSha256(sumsText, assetName)
            ?: return VerifyResult.Failed("no checksum for $assetName in SHA256SUMS")
        val actual = sha256(pkg)
        if (!constantTimeEquals(expected.lowercase(), actual.lowercase()))
            return VerifyResult.Failed("checksum mismatch for $assetName")

        return VerifyResult.Ok
    }

    /** Verifies an ASCII-armored detached OpenPGP signature over [data]. */
    fun verifyDetached(data: ByteArray, sigArmored: ByteArray, pubKey: ByteArray): Boolean {
        val factory = JcaPGPObjectFactory(PGPUtil.getDecoderStream(sigArmored.inputStream()))
        val sigList = factory.nextObject() as? PGPSignatureList ?: return false
        if (sigList.isEmpty) return false
        val sig = sigList[0]

        val rings = JcaPGPPublicKeyRingCollection(PGPUtil.getDecoderStream(pubKey.inputStream()))
        val key = rings.getPublicKey(sig.keyID) ?: return false

        sig.init(JcaPGPContentVerifierBuilderProvider().setProvider(provider), key)
        sig.update(data)
        return sig.verify()
    }

    /** Returns the hex digest for [fileName] from a `sha256sum`-format file. */
    fun expectedSha256(sumsText: String, fileName: String): String? =
        sumsText.lineSequence()
            .mapNotNull { line ->
                // "<hex>␠␠<name>" (binary marker "*" possible after the spaces).
                val parts = line.trim().split(Regex("\\s+"), limit = 2)
                if (parts.size == 2) parts[0] to parts[1].removePrefix("*") else null
            }
            .firstOrNull { it.second == fileName }
            ?.first

    fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { ins ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = ins.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var r = 0
        for (i in a.indices) r = r or (a[i].code xor b[i].code)
        return r == 0
    }
}
