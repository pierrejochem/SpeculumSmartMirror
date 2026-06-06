package org.speculum.core

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import org.speculum.config.ModuleConfig

/**
 * Base contract for every module, modeled on MagicMirror's Module.register API.
 * Lifecycle: start() -> (loop refresh) -> Content() rendered -> stop().
 */
abstract class MirrorModule(
    val config: ModuleConfig
) : NotificationListener {

    val name: String get() = config.module
    val region: Region get() = config.regionEnum
    open val refreshIntervalMs: Long get() = config.refreshIntervalMs

    open fun start(scope: CoroutineScope) {}
    open suspend fun refresh() {}
    open fun stop() {}
    override fun onNotification(notification: Notification) {}

    @Composable
    abstract fun Content()
}