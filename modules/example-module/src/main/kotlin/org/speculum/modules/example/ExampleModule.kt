package org.speculum.modules.example

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.speculum.config.ModuleConfig
import org.speculum.core.MirrorModule
import org.speculum.core.Notification
import org.speculum.ui.MirrorColors

/**
 * Reference external module, packaged as its own JAR and loaded at runtime
 * via the [ExampleModuleFactory] service. Exercises the full module API —
 * see DEVELOPER_GUIDE.md.
 *
 * Config keys: greeting (default "Hello, Speculum!"), tickStep (default 1).
 */
class ExampleModule(config: ModuleConfig) : MirrorModule(config) {

    private val greeting = config.string("greeting", "Hello, Speculum!")
    private val tickStep = config.int("tickStep", 1)

    private var ticks by mutableStateOf(0)
    private var lastEvent by mutableStateOf("(none)")

    override val refreshIntervalMs: Long = config.refreshIntervalMs.coerceAtLeast(1000)

    override fun start(scope: CoroutineScope) {
        scope.launch {
            while (true) {
                delay(refreshIntervalMs)
                // background work hook
            }
        }
    }

    override suspend fun refresh() {
        ticks += tickStep
    }

    override fun onNotification(notification: Notification) {
        lastEvent = notification.name
    }

    @Composable
    override fun Content() {
        Column {
            Text(greeting, color = Color.White, fontSize = 28.sp)
            Text("Refreshes: $ticks", color = MirrorColors.Normal, fontSize = 18.sp)
            Text("Last notification: $lastEvent", color = MirrorColors.Dimmed, fontSize = 14.sp)
        }
    }

    override fun stop() { /* nothing to release */ }
}