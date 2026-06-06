package org.speculum.core

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.speculum.config.MirrorConfig

/**
 * The runtime that loads modules from config, starts their lifecycles,
 * drives per-module refresh loops, and brokers the notification bus.
 *
 * Boot is split so a splash screen can await real readiness:
 *  1. [createModules] — instantiate + start() each module
 *  2. [refreshAll]    — run the first refresh of every module, awaited
 *  3. [startLoops]    — begin periodic refresh loops
 *
 * Owns a child scope so [shutdown] fully cancels its coroutines (hot-reload).
 */
class MirrorEngine(
    private val config: MirrorConfig,
    parentScope: CoroutineScope
) {
    private val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob())
    val modules = mutableStateListOf<MirrorModule>()

    fun createModules(onLoaded: (MirrorModule) -> Unit = {}) {
        config.modules.forEach { mc ->
            ModuleRegistry.create(mc)?.let { module ->
                modules.add(module)
                module.start(scope)
                onLoaded(module)
            }
        }
        broadcast(Notification("ALL_MODULES_STARTED", sender = "core"))
    }

    /** First data fetch of every module, in parallel; returns when all settle. */
    suspend fun refreshAll(onUpdated: (MirrorModule) -> Unit = {}) = coroutineScope {
        modules.map { module ->
            async {
                runCatching { module.refresh() }
                onUpdated(module)
            }
        }.awaitAll()
        Unit
    }

    /** Periodic refresh loops (first tick already done by [refreshAll]). */
    fun startLoops() {
        modules.forEach { module ->
            if (module.refreshIntervalMs <= 0) return@forEach
            scope.launch {
                while (true) {
                    delay(module.refreshIntervalMs)
                    runCatching { module.refresh() }
                }
            }
        }
    }

    /** Deliver a notification to every loaded module (the bus). */
    fun broadcast(notification: Notification) {
        modules.forEach { it.onNotification(notification) }
    }

    fun shutdown() {
        modules.forEach { it.stop() }
        modules.clear()
        scope.cancel()
    }
}
