package org.speculum

import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.jetbrains.compose.resources.painterResource
import org.speculum.configserver.startServer
import org.speculum.resources.Res
import org.speculum.resources.speculum_icon

/**
 * Desktop entry point. Starts the config web admin in-process (background
 * threads) so one package serves both the fullscreen mirror and the admin UI,
 * then opens the mirror window. Set MIRROR_ADMIN_DISABLED=1 to skip the server.
 */
fun main() {
    // Skiko defaults to OpenGL on Linux, which renders a black screen on many
    // Linux GPU/driver/Wayland combos (notably KDE). The mirror UI is mostly
    // static, so software rendering is a reliable default here. Respect an
    // explicit override via the `skiko.renderApi` property or `SKIKO_RENDER_API`
    // env var (e.g. set OPENGL to force GPU rendering).
    val osIsLinux = System.getProperty("os.name").orEmpty().startsWith("Linux", ignoreCase = true)
    if (osIsLinux &&
        System.getProperty("skiko.renderApi") == null &&
        System.getenv("SKIKO_RENDER_API") == null
    ) {
        System.setProperty("skiko.renderApi", "SOFTWARE")
    }

    if (System.getenv("MIRROR_ADMIN_DISABLED") == null) {
        runCatching { startServer(wait = false) }
            .onFailure { System.err.println("[admin] config server failed to start: $it") }
    }

    application {
        val windowState = rememberWindowState(
            placement = WindowPlacement.Fullscreen,
            position = WindowPosition(Alignment.Center)
        )
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Speculum",
            icon = painterResource(Res.drawable.speculum_icon)
        ) {
            App()
        }
    }
}