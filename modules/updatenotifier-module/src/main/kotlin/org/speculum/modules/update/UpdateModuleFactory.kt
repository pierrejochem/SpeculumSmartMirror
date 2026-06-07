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
        // currentVersion is auto-detected from the running app (system property /
        // manifest). It's surfaced here so the admin can display it, but it's
        // shown read-only there — editing it has no lasting effect.
        config = mapOf(
            "repo" to "pierrejochem/Speculum",
            "currentVersion" to detectVersion(),
        ),
    )

    // Negative so the notifier banner stacks above other top_center modules.
    override val order: Int get() = -100
}