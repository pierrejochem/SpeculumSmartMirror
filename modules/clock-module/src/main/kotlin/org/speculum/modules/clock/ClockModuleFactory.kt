package org.speculum.modules.clock

import org.speculum.config.ModuleConfig
import org.speculum.core.MirrorModule
import org.speculum.core.ModuleFactory

class ClockModuleFactory : ModuleFactory {
    override val name = "clock"
    override val order = 0
    override fun create(config: ModuleConfig): MirrorModule = ClockModule(config)
    override fun defaultConfig() = ModuleConfig(
        module = "clock",
        position = "top_left",
        refreshIntervalMs = 0,
        config = mapOf(
            "timeFormat" to "24",
            "displaySeconds" to "true",
            "showDate" to "true",
            "displayType" to "digital",
        )
    )
}