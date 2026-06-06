package org.speculum.modules.compliments

import org.speculum.config.ModuleConfig
import org.speculum.core.MirrorModule
import org.speculum.core.ModuleFactory

class ComplimentsModuleFactory : ModuleFactory {
    override val name = "compliments"
    override fun create(config: ModuleConfig): MirrorModule = ComplimentsModule(config)
    override fun defaultConfig() = ModuleConfig(
        module = "compliments",
        position = "lower_third",
        refreshIntervalMs = 0,
        config = mapOf("updateInterval" to "30")
    )
}