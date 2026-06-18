package org.speculum.configserver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.speculum.update.Downloader
import org.speculum.update.GitHubReleaseProvider
import org.speculum.update.InstallType
import org.speculum.update.SignatureVerifier
import org.speculum.update.VerifyResult
import org.speculum.update.selectAssetByName
import org.speculum.update.selectPackageAsset
import java.io.File

/** Polled snapshot for `GET /api/update/progress`. */
@Serializable
data class UpdateProgress(
    val phase: String,
    val pct: Int? = null,
    val message: String = "",
    val targetVersion: String? = null,
)

/**
 * In-memory, single-flight update job. Downloads + verifies a release as the
 * unprivileged mirror user, stages it, then triggers the root helper which
 * installs it and restarts the service (killing this process). No persistence:
 * a restart means either success (new version answers) or the job never reached
 * install; the root helper's `result.json` is the only cross-restart breadcrumb.
 */
object UpdateJob {

    enum class Phase { IDLE, DOWNLOADING, VERIFYING, INSTALLING, RESTARTING, INSTALLED, ERROR }

    @Volatile private var phase = Phase.IDLE
    @Volatile private var pct: Int? = null
    @Volatile private var message = ""
    @Volatile private var target: String? = null

    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun snapshot() = UpdateProgress(phase.name.lowercase(), pct, message, target)

    /** Starts a job. Returns false if one is already running (caller → 409). */
    fun start(): Boolean {
        if (!mutex.tryLock()) return false
        phase = Phase.DOWNLOADING; pct = 0; message = "Starting…"; target = null
        scope.launch {
            try {
                run()
            } catch (e: Throwable) {
                fail(e.message ?: "update failed")
            } finally {
                mutex.unlock()
            }
        }
        return true
    }

    private suspend fun run() {
        val http = GitHubReleaseProvider.defaultClient()
        val dlHttp = Downloader.downloadClient()
        try {
            val release = GitHubReleaseProvider(UpdateService.repo(), http).latest()
                ?: return fail("Couldn't reach GitHub.")
            val format = InstallType.format() ?: return fail("Not a packaged install.")
            val osArch = System.getProperty("os.arch").orEmpty()

            val pkgAsset = selectPackageAsset(release.assets, format, osArch)
                ?: return fail("No matching package for this system.")
            val sumsAsset = selectAssetByName(release.assets, "SHA256SUMS")
                ?: return fail("Release has no SHA256SUMS.")
            val sigAsset = selectAssetByName(release.assets, "SHA256SUMS.asc")
                ?: return fail("Release is unsigned — refusing to update.")
            val key = UpdateService.pinnedKey() ?: return fail("Signing key unavailable.")
            target = release.tagName.removePrefix("v")

            val staging = UpdateService.stagingDir
            if (!staging.isDirectory && !staging.mkdirs())
                return fail("Couldn't create the update staging dir.")
            if (staging.usableSpace < pkgAsset.size * 2)
                return fail("Not enough disk space for the update.")

            val pkgFile = File(staging, "staged.pkg")
            val sumsFile = File(staging, "SHA256SUMS")
            val sigFile = File(staging, "SHA256SUMS.asc")
            File(staging, "result.json").delete()

            val dl = Downloader(dlHttp)
            message = "Downloading ${pkgAsset.name}…"
            dl.download(pkgAsset.downloadUrl, pkgFile) { read, total ->
                pct = total?.takeIf { it > 0 }?.let { (read * 100 / it).toInt() }
            }
            dl.download(sumsAsset.downloadUrl, sumsFile)
            dl.download(sigAsset.downloadUrl, sigFile)

            phase = Phase.VERIFYING; pct = null; message = "Verifying signature…"
            val result = SignatureVerifier.verify(sumsFile.readText(), sigFile.readBytes(), key, pkgFile, pkgAsset.name)
            if (result is VerifyResult.Failed) {
                pkgFile.delete()
                return fail("Verification failed: ${result.reason}")
            }

            File(staging, "staged.meta").writeText(
                json.encodeToString(
                    StagedMeta(pkgAsset.name, SignatureVerifier.sha256(pkgFile), target!!, format.name.lowercase())
                )
            )

            phase = Phase.INSTALLING; message = "Installing — Speculum will restart…"
            // Dwell so the UI observes INSTALLING before a restart SIGTERMs us.
            delay(1500)

            if (!trigger()) return fail("Couldn't authorize the update (polkit/systemd).")

            phase = Phase.RESTARTING; message = "Applying update…"
            // If the service is active the helper restarts it and kills us here
            // (expected — the UI reconnects via /api/version). If launched manually
            // the helper writes result.json and we survive: surface "relaunch".
            val resultFile = File(staging, "result.json")
            repeat(120) {
                if (resultFile.exists()) {
                    val body = runCatching { resultFile.readText() }.getOrDefault("")
                    if (body.contains("\"error\"")) {
                        val msg = Regex("\"message\"\\s*:\\s*\"([^\"]*)\"").find(body)?.groupValues?.get(1)
                        return fail(msg?.takeIf { it.isNotBlank() } ?: "Update failed during install.")
                    }
                    phase = Phase.INSTALLED
                    message = "Update installed — relaunch Speculum to apply."
                    return
                }
                delay(500)
            }
        } finally {
            http.close()
            dlHttp.close()
        }
    }

    /** Triggers the root oneshot unit (authorized for user `pi` via polkit). */
    private fun trigger(): Boolean = runCatching {
        ProcessBuilder("systemctl", "--no-block", "start", "speculum-update.service")
            .redirectErrorStream(true)
            .start()
            .waitFor() == 0
    }.getOrDefault(false)

    private fun fail(reason: String) {
        phase = Phase.ERROR; pct = null; message = reason
    }
}
