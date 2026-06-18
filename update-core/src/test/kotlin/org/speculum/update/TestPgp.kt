package org.speculum.update

import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.HashAlgorithmTags
import org.bouncycastle.bcpg.PublicKeyPacket
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.PGPKeyRingGenerator
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPSignatureGenerator
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder
import java.io.ByteArrayOutputStream
import java.security.KeyPairGenerator
import java.security.Security
import java.util.Date

/** Test-only helper: a real OpenPGP key ring to sign with and verify against. */
object TestPgp {
    init { if (Security.getProvider("BC") == null) Security.addProvider(BouncyCastleProvider()) }

    class Material(val publicKeyArmored: ByteArray, val secretRing: PGPSecretKeyRing)

    fun generate(): Material {
        val kpg = KeyPairGenerator.getInstance("RSA", "BC").apply { initialize(2048) }
        val pgpKeyPair =
            JcaPGPKeyPair(PublicKeyPacket.VERSION_4, PGPPublicKey.RSA_GENERAL, kpg.generateKeyPair(), Date())
        val sha1 = JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA1)
        val ringGen = PGPKeyRingGenerator(
            PGPSignature.POSITIVE_CERTIFICATION,
            pgpKeyPair,
            "Test <test@example.com>",
            sha1,
            null,
            null,
            JcaPGPContentSignerBuilder(PGPPublicKey.RSA_GENERAL, HashAlgorithmTags.SHA256),
            null,
        )
        val out = ByteArrayOutputStream()
        ArmoredOutputStream(out).use { ringGen.generatePublicKeyRing().encode(it) }
        return Material(out.toByteArray(), ringGen.generateSecretKeyRing())
    }

    fun signArmored(data: ByteArray, secRing: PGPSecretKeyRing): ByteArray {
        val secretKey = secRing.secretKey
        val privateKey = secretKey.extractPrivateKey(
            JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(CharArray(0))
        )
        val sigGen = PGPSignatureGenerator(
            JcaPGPContentSignerBuilder(secretKey.publicKey.algorithm, HashAlgorithmTags.SHA256).setProvider("BC"),
            secretKey.publicKey,
        )
        sigGen.init(PGPSignature.BINARY_DOCUMENT, privateKey)
        sigGen.update(data)
        val out = ByteArrayOutputStream()
        ArmoredOutputStream(out).use { sigGen.generate().encode(it) }
        return out.toByteArray()
    }
}
