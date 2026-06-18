package org.speculum.update

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AssetMatcherTest {

    private val assets = listOf(
        ReleaseAsset("speculum_0.4.2_arm64.deb", "u/deb-arm", 1),
        ReleaseAsset("speculum_0.4.2_amd64.deb", "u/deb-amd", 1),
        ReleaseAsset("speculum_0.4.2_arm64.rpm", "u/rpm-arm", 1),
        ReleaseAsset("speculum_0.4.2_amd64.rpm", "u/rpm-amd", 1),
        ReleaseAsset("speculum-0.4.2-1-aarch64.pkg.tar.xz", "u/arch-arm", 1),
        ReleaseAsset("speculum-0.4.2-1-x86_64.pkg.tar.zst", "u/arch-amd", 1),
        ReleaseAsset("SHA256SUMS", "u/sums", 1),
        ReleaseAsset("SHA256SUMS.asc", "u/sig", 1),
    )

    @Test
    fun archMapping() {
        assertEquals(TargetArch("arm64", "aarch64"), archFor("aarch64"))
        assertEquals(TargetArch("amd64", "x86_64"), archFor("amd64"))
        assertEquals(TargetArch("amd64", "x86_64"), archFor("x86_64"))
        assertNull(archFor("arm")) // armv7 etc. unsupported
    }

    @Test
    fun debSelection() {
        assertEquals("u/deb-arm", selectPackageAsset(assets, InstallFormat.DEB, "aarch64")?.downloadUrl)
        assertEquals("u/deb-amd", selectPackageAsset(assets, InstallFormat.DEB, "amd64")?.downloadUrl)
    }

    @Test
    fun rpmUsesDebianArchInName() {
        // The rpm asset is named with the Debian arch (arm64), not aarch64.
        assertEquals("u/rpm-arm", selectPackageAsset(assets, InstallFormat.RPM, "aarch64")?.downloadUrl)
        assertEquals("u/rpm-amd", selectPackageAsset(assets, InstallFormat.RPM, "amd64")?.downloadUrl)
    }

    @Test
    fun archMatchesBothCompressions() {
        // aarch64 ships .xz, x86_64 ships .zst — both must match.
        assertEquals("u/arch-arm", selectPackageAsset(assets, InstallFormat.ARCH, "aarch64")?.downloadUrl)
        assertEquals("u/arch-amd", selectPackageAsset(assets, InstallFormat.ARCH, "x86_64")?.downloadUrl)
    }

    @Test
    fun unsupportedArchSelectsNothing() {
        assertNull(selectPackageAsset(assets, InstallFormat.DEB, "armv7l"))
    }

    @Test
    fun byNameExact() {
        assertEquals("u/sums", selectAssetByName(assets, "SHA256SUMS")?.downloadUrl)
        assertEquals("u/sig", selectAssetByName(assets, "SHA256SUMS.asc")?.downloadUrl)
        assertNull(selectAssetByName(assets, "nope"))
    }

    @Test
    fun doesNotCrossMatchFormats() {
        // A deb regex must not match an rpm/arch asset for the same arch.
        val onlyRpm = assets.filter { it.name.endsWith(".rpm") }
        assertNull(selectPackageAsset(onlyRpm, InstallFormat.DEB, "aarch64"))
    }
}
