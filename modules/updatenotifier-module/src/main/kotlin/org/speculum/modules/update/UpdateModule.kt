package org.speculum.modules.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.speculum.config.ModuleConfig
import org.speculum.core.MirrorModule

/**
 * Shows the update status of the running mirror against the repo's latest
 * GitHub release. When a newer release exists it shows an "Update available"
 * banner with the new version; otherwise a discreet up-to-date hint with the
 * installed version. Compares the running version (`currentVersion` config)
 * against the latest release tag.
 *
 * The running version is detected automatically (the host sets the
 * `speculum.version` system property from the build); `currentVersion` config
 * only overrides it when set.
 *
 * Config keys:
 *  - `repo`            GitHub "owner/name"  (default "pierrejochem/Speculum")
 *  - `currentVersion`  override the auto-detected installed version (optional)
 */
class UpdateModule(config: ModuleConfig) : MirrorModule(config) {

    private val repo = config.string("repo", "pierrejochem/Speculum")
    private val current = detectVersion(config.string("currentVersion", ""))
    private val dev = isDevVersion(current)
    private val provider = GitHubReleaseProvider(repo)

    // Don't hammer the GitHub API — at least 6h between checks.
    override val refreshIntervalMs: Long =
        config.refreshIntervalMs.coerceAtLeast(6 * 60 * 60_000L)

    private var update by mutableStateOf<UpdateInfo?>(null)
    private var checked by mutableStateOf(false)

    override suspend fun refresh() {
        if (dev) { checked = true; return } // no release comparison for dev builds
        val release = provider.latest()
        update = if (release != null && release.tagName.isNotBlank() &&
            isNewer(release.tagName, current)
        ) {
            UpdateInfo(release.tagName.removePrefix("v"), release.htmlUrl)
        } else {
            null
        }
        checked = true
    }

    @Composable
    override fun Content() {
        val u = update
        when {
            // Development build — show "dev", never nag about releases.
            dev -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Speculum dev", color = Color.White, fontSize = 16.sp)
                Text("Development build", color = DIM, fontSize = 14.sp)
            }

            // Before the first check completes.
            !checked -> Text("Checking for updates…", color = DIM, fontSize = 14.sp)

            // A newer release is available.
            u != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⇈", color = ACCENT, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Update available",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Text(
                    "v${u.version}  ·  installed v$current",
                    color = DIM,
                    fontSize = 14.sp,
                )
            }

            // Up to date — show the current version as a quiet hint.
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Speculum v$current", color = Color.White, fontSize = 16.sp)
                Text("No update available", color = DIM, fontSize = 14.sp)
            }
        }
    }

    private companion object {
        val DIM = Color(0xFF999999)
        val ACCENT = Color(0xFF3DD4C8) // brand teal
    }
}