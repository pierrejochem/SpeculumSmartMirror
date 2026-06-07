package org.speculum.modules.update

import org.speculum.config.ModuleConfig
import org.speculum.core.MirrorModule
import org.speculum.core.ModuleFactory

class UpdateModuleFactory : ModuleFactory {
    override val name = "updatenotifier"

    override fun create(config: ModuleConfig): MirrorModule = UpdateModule(config)

    override fun defaultConfig(): ModuleConfig = ModuleConfig(
        module = "updatenotifier",
        position = "top_center",
        refreshIntervalMs = 6 * 60 * 60_000L, // 6h
        config = mapOf(
            "repo" to "pierrejochem/Speculum",
            "currentVersion" to "1.0.0",
        ),
    )

    // Negative so the notifier banner stacks above other top_center modules.
    override val order: Int get() = -100
}