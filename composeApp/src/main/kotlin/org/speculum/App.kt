package org.speculum

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import org.speculum.config.ConfigLoader
import org.speculum.core.MirrorEngine
import org.speculum.core.ModuleRegistry
import org.speculum.core.discoverPluginFactories
import org.speculum.ui.MirrorColors
import org.speculum.ui.MirrorScreen
import org.speculum.ui.SplashScreen
import org.speculum.ui.robotoFamily

/**
 * App root: shows a bootstrap splash while the engine loads modules and fetches
 * their first data, then swaps to the mirror. Watches `config.json` and
 * hot-reloads the engine in-place afterwards (no splash on reload).
 */
@Composable
fun App() {
    val scope = rememberCoroutineScope()
    var engine by remember { mutableStateOf<MirrorEngine?>(null) }
    val bootLog = remember { mutableStateListOf<String>() }
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        // Initial boot — with the splash log + progress.
        engine = bootEngine(scope) { msg, frac -> bootLog.add(msg); progress = frac }

        // Then watch the config file and hot-reload silently on change (debounced).
        var stamp = ConfigLoader.lastModified()
        while (true) {
            delay(1000)
            var now = ConfigLoader.lastModified()
            if (now == stamp) continue
            do { val prev = now; delay(500); now = ConfigLoader.lastModified() } while (now != prev)
            stamp = now
            val fresh = bootEngine(scope)   // build + fetch before swapping (no blank gap)
            engine?.shutdown()
            engine = fresh
        }
    }
    DisposableEffect(Unit) { onDispose { engine?.shutdown() } }

    val roboto = robotoFamily()
    MaterialTheme(colorScheme = darkColorScheme()) {
        CompositionLocalProvider(
            LocalTextStyle provides TextStyle(fontFamily = roboto, color = MirrorColors.Bright)
        ) {
            val current = engine
            if (current == null) SplashScreen(bootLog, progress) else MirrorScreen(current)
        }
    }
}

/**
 * Builds + fully boots an engine from the current config (a non-empty module
 * list is authoritative; empty = all plugin defaults), reporting progress.
 * Returns only once every module's first refresh has completed.
 */
private suspend fun bootEngine(
    scope: CoroutineScope,
    onProgress: (message: String, fraction: Float) -> Unit = { _, _ -> }
): MirrorEngine {
    onProgress("Discovering modules…", 0.05f)
    val plugins = discoverPluginFactories()
    plugins.forEach { ModuleRegistry.register(it) }

    onProgress("Loading configuration…", 0.10f)
    val base = ConfigLoader.load()
    val config = if (base.modules.isNotEmpty()) base
    else base.copy(modules = plugins.sortedBy { it.order }.mapNotNull { it.defaultConfig() })

    val n = config.modules.size.coerceAtLeast(1)
    val engine = MirrorEngine(config, scope)

    var created = 0
    engine.createModules { onProgress("Started ${it.name}", 0.10f + 0.30f * (++created) / n) }

    onProgress("Fetching data…", 0.40f)
    var done = 0
    engine.refreshAll { onProgress("Ready: ${it.name}", 0.40f + 0.55f * (++done) / n) }

    engine.startLoops()
    onProgress("Ready", 1f)
    println("[mirror] boot modules=${config.modules.map { it.module }}")
    return engine
}
