package org.speculum.update

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class SignatureVerifierTest {

    private val asset = "speculum_0.4.2_arm64.deb"

    @Test
    fun parsesChecksumLines() {
        val sums = """
            aaaa1111  speculum_0.4.2_arm64.deb
            bbbb2222 *speculum_0.4.2_amd64.deb
        """.trimIndent()
        assertEquals("aaaa1111", SignatureVerifier.expectedSha256(sums, "speculum_0.4.2_arm64.deb"))
        assertEquals("bbbb2222", SignatureVerifier.expectedSha256(sums, "speculum_0.4.2_amd64.deb"))
        assertNull(SignatureVerifier.expectedSha256(sums, "missing.deb"))
    }

    @Test
    fun sha256MatchesKnown(@TempDir dir: Path) {
        val f = File(dir.toFile(), "x").apply { writeText("abc") }
        // sha256("abc")
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            SignatureVerifier.sha256(f),
        )
    }

    @Test
    fun validSignatureAndChecksum_ok(@TempDir dir: Path) {
        val mat = TestPgp.generate()
        val pkg = File(dir.toFile(), asset).apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val sums = "${SignatureVerifier.sha256(pkg)}  $asset\n"
        val sig = TestPgp.signArmored(sums.toByteArray(), mat.secretRing)

        val result = SignatureVerifier.verify(sums, sig, mat.publicKeyArmored, pkg, asset)
        assertInstanceOf(VerifyResult.Ok::class.java, result)
    }

    @Test
    fun tamperedSums_rejected(@TempDir dir: Path) {
        val mat = TestPgp.generate()
        val pkg = File(dir.toFile(), asset).apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val sums = "${SignatureVerifier.sha256(pkg)}  $asset\n"
        val sig = TestPgp.signArmored(sums.toByteArray(), mat.secretRing)
        // Verify with a DIFFERENT sums body than what was signed.
        val tampered = "deadbeef  $asset\n"

        val result = SignatureVerifier.verify(tampered, sig, mat.publicKeyArmored, pkg, asset)
        assertInstanceOf(VerifyResult.Failed::class.java, result)
    }

    @Test
    fun checksumMismatch_rejected(@TempDir dir: Path) {
        val mat = TestPgp.generate()
        val pkg = File(dir.toFile(), asset).apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        // Sign a sums file whose hash does NOT match the package.
        val sums = "deadbeef  $asset\n"
        val sig = TestPgp.signArmored(sums.toByteArray(), mat.secretRing)

        val result = SignatureVerifier.verify(sums, sig, mat.publicKeyArmored, pkg, asset)
        assertTrue(result is VerifyResult.Failed && result.reason.contains("checksum"))
    }

    @Test
    fun missingSignature_failClosed(@TempDir dir: Path) {
        val pkg = File(dir.toFile(), asset).apply { writeBytes(byteArrayOf(1)) }
        val sums = "${SignatureVerifier.sha256(pkg)}  $asset\n"
        val mat = TestPgp.generate()

        assertInstanceOf(
            VerifyResult.Failed::class.java,
            SignatureVerifier.verify(sums, null, mat.publicKeyArmored, pkg, asset),
        )
        assertInstanceOf(
            VerifyResult.Failed::class.java,
            SignatureVerifier.verify(sums, ByteArray(0), mat.publicKeyArmored, pkg, asset),
        )
    }

    @Test
    fun garbageSignature_failClosed(@TempDir dir: Path) {
        val pkg = File(dir.toFile(), asset).apply { writeBytes(byteArrayOf(1)) }
        val sums = "${SignatureVerifier.sha256(pkg)}  $asset\n"
        val mat = TestPgp.generate()

        val result = SignatureVerifier.verify(sums, "not a signature".toByteArray(), mat.publicKeyArmored, pkg, asset)
        assertInstanceOf(VerifyResult.Failed::class.java, result)
    }
}
