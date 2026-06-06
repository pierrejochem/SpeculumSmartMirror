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