package org.speculum.update

/** Package format this install was delivered as (drives which asset to fetch). */
enum class InstallFormat { DEB, RPM, ARCH }

/** Architecture naming: Debian uses arm64/amd64, Arch uses aarch64/x86_64. */
data class TargetArch(val debArch: String, val pkgArch: String)

/**
 * Maps the JVM's `os.arch` to release-asset arch names. Returns null for any
 * arch we don't publish packages for (e.g. armv7) — the updater is then "not
 * available" rather than guessing.
 */
fun archFor(osArch: String): TargetArch? = when (osArch.lowercase()) {
    "aarch64", "arm64" -> TargetArch(debArch = "arm64", pkgArch = "aarch64")
    "amd64", "x86_64", "x86-64" -> TargetArch(debArch = "amd64", pkgArch = "x86_64")
    else -> null
}

/**
 * Anchored regex matching the release asset for a given [format] and arch. The
 * version segment is left loose (`.+`) because the asset name carries data not
 * present in the tag — notably the Arch `pkgrel` and the per-arch compression
 * (`.zst` vs `.xz`). NOTE: the rpm asset is named with the *Debian* arch
 * (`speculum_<ver>_arm64.rpm`), matching the release workflow.
 */
fun assetRegex(format: InstallFormat, arch: TargetArch): Regex = when (format) {
    InstallFormat.DEB -> Regex("""^speculum_.+_${arch.debArch}\.deb$""")
    InstallFormat.RPM -> Regex("""^speculum_.+_${arch.debArch}\.rpm$""")
    InstallFormat.ARCH -> Regex("""^speculum-.+-${arch.pkgArch}\.pkg\.tar\.(zst|xz)$""")
}

/** Picks the package asset for this install's [format] + [osArch], or null. */
fun selectPackageAsset(
    assets: List<ReleaseAsset>,
    format: InstallFormat,
    osArch: String,
): ReleaseAsset? {
    val arch = archFor(osArch) ?: return null
    val re = assetRegex(format, arch)
    return assets.firstOrNull { re.matches(it.name) }
}

/** Picks an asset by exact name (e.g. "SHA256SUMS", "SHA256SUMS.asc"). */
fun selectAssetByName(assets: List<ReleaseAsset>, name: String): ReleaseAsset? =
    assets.firstOrNull { it.name == name }
