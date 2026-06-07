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
 * Shows a discreet banner when a newer Speculum release exists on GitHub.
 * Compares the running version (`currentVersion` config) against the repo's
 * latest release tag. When up to date it renders nothing, so it stays invisible
 * on the mirror until there's actually something to act on.
 *
 * Config keys:
 *  - `repo`            GitHub "owner/name"  (default "pierrejochem/Speculum")
 *  - `currentVersion`  the installed version (default "1.0.0")
 */
class UpdateModule(config: ModuleConfig) : MirrorModule(config) {

    private val repo = config.string("repo", "pierrejochem/Speculum")
    private val current = config.string("currentVersion", "1.0.0")
    private val provider = GitHubReleaseProvider(repo)

    // Don't hammer the GitHub API — at least 6h between checks.
    override val refreshIntervalMs: Long =
        config.refreshIntervalMs.coerceAtLeast(6 * 60 * 60_000L)

    private var update by mutableStateOf<UpdateInfo?>(null)

    override suspend fun refresh() {
        val release = provider.latest()
        update = if (release != null && release.tagName.isNotBlank() &&
            isNewer(release.tagName, current)
        ) {
            UpdateInfo(release.tagName.removePrefix("v"), release.htmlUrl)
        } else {
            null
        }
    }

    @Composable
    override fun Content() {
        val u = update ?: return // up to date → render nothing
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⬆︎", color = ACCENT, fontSize = 18.sp) // ⬆ (text glyph)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Update available",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(
                "v${u.version}  ·  installed v${current.removePrefix("v")}",
                color = DIM,
                fontSize = 14.sp,
            )
        }
    }

    private companion object {
        val DIM = Color(0xFF999999)
        val ACCENT = Color(0xFF3DD4C8) // brand teal
    }
}