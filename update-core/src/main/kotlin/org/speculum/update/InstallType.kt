package org.speculum.update

import java.io.File

/**
 * Resolves how this app was installed, for the self-updater. Everything keys off
 * `jpackage.app-path` — the launcher path the jpackage native binary sets only
 * for a packaged install (e.g. `/opt/speculum/bin/speculum`). Dev runs return
 * null everywhere, disabling the in-app updater (Decision: packaged-only).
 */
object InstallType {

    /** The package format marker each maintainer script drops at install time. */
    private const val MARKER = ".install-format"

    /** Install root, e.g. `/opt/speculum` — `<launcher>/../..`. Null in dev. */
    fun installRoot(): File? {
        val appPath = System.getProperty("jpackage.app-path")?.takeIf { it.isNotBlank() } ?: return null
        // <root>/bin/speculum -> <root>
        return File(appPath).parentFile?.parentFile
    }

    /**
     * The package format this install was delivered as, read from the marker the
     * maintainer script wrote (deb/rpm via postinstall, arch via PKGBUILD). Null
     * when running in dev, on an unmarked/manual install, or an unknown value.
     */
    fun format(): InstallFormat? {
        val root = installRoot() ?: return null
        val raw = runCatching { File(root, MARKER).readText().trim().lowercase() }.getOrNull()
        return when (raw) {
            "deb" -> InstallFormat.DEB
            "rpm" -> InstallFormat.RPM
            "arch" -> InstallFormat.ARCH
            else -> null
        }
    }

    /** Root-readable pinned signing key the privileged helper verifies against. */
    fun signingKeyFile(): File? = installRoot()?.let { File(it, "share/speculum-signing-key.asc") }
}
