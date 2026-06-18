package org.speculum.configserver

import io.ktor.client.HttpClient
import kotlinx.serialization.Serializable
import org.speculum.update.GitHubReleaseProvider
import org.speculum.update.InstallType
import org.speculum.update.detectVersion
import org.speculum.update.isDevVersion
import org.speculum.update.isNewer
import java.io.File

/** Reply for `GET /api/update/status` — drives the Updates card. */
@Serializable
data class UpdateStatus(
    val currentVersion: String,
    val latestVersion: String? = null,
    val updateAvailable: Boolean = false,
    val updatable: Boolean = false,
    val installType: String = "unsupported",
    val signed: Boolean = false,
    val releaseUrl: String? = null,
    val reason: String? = null,
)

/** Read-only metadata persisted next to the staged package for the root helper. */
@Serializable
data class StagedMeta(val filename: String, val sha256: String, val version: String, val format: String)

/**
 * Shared paths/inputs for the in-app updater. The mirror runs unprivileged; this
 * only *prepares and verifies* an update — the privileged install happens in the
 * root [speculum-update.service] helper (see UpdateJob).
 */
object UpdateService {

    /**
     * Staging dir the unprivileged mirror writes verified packages into — under
     * its own home (always writable, no special group needed). The root helper
     * locates it by scanning home dirs, so the two never disagree on a user.
     */
    val stagingDir: File = File(System.getProperty("user.home"), ".speculum/update")

    /** One shared HTTP client — status() is polled, so don't leak a client per call. */
    private val http: HttpClient by lazy { GitHubReleaseProvider.defaultClient() }

    /** GitHub "owner/name": the updatenotifier module's repo, else env, else default. */
    fun repo(): String =
        runCatching {
            ConfigStore.load().modules.firstOrNull { it.module == "updatenotifier" }?.config?.get("repo")
        }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: System.getenv("MIRROR_UPDATE_REPO")
            ?: "pierrejochem/SpeculumSmartMirror"

    /** Pinned signing public key bundled in the app resources (for the app-side check). */
    fun pinnedKey(): ByteArray? {
        val dir = System.getProperty("compose.application.resources.dir")?.let(::File) ?: return null
        return File(dir, "speculum-signing-key.asc").takeIf { it.isFile }?.readBytes()
    }

    suspend fun status(): UpdateStatus {
        val current = detectVersion()
        val format = InstallType.format()
        val release = runCatching { GitHubReleaseProvider(repo(), http).latest() }.getOrNull()
        val latest = release?.tagName?.removePrefix("v")?.takeIf { it.isNotBlank() }
        val signed = release?.assets?.any { it.name == "SHA256SUMS.asc" } == true
        val newer = latest != null && isNewer(latest, current)
        val packaged = format != null && !isDevVersion(current)
        val updatable = packaged && newer && signed && pinnedKey() != null

        val reason = when {
            isDevVersion(current) -> "Development build — update via the releases page."
            format == null -> "Not a packaged install — update via the releases page."
            latest == null -> "Couldn't check GitHub for updates."
            !newer -> null
            !signed -> "Latest release is unsigned — install it manually for now."
            pinnedKey() == null -> "Signing key unavailable — cannot verify updates."
            else -> null
        }
        return UpdateStatus(
            currentVersion = current,
            latestVersion = latest,
            updateAvailable = newer,
            updatable = updatable,
            installType = format?.name?.lowercase() ?: "unsupported",
            signed = signed,
            releaseUrl = release?.htmlUrl?.takeIf { it.isNotBlank() },
            reason = reason,
        )
    }
}
