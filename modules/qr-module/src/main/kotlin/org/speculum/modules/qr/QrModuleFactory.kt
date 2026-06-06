package org.speculum.modules.qr

import org.speculum.config.ModuleConfig
import org.speculum.core.MirrorModule
import org.speculum.core.ModuleFactory

class QrModuleFactory : ModuleFactory {
    override val name = "qr"
    override fun create(config: ModuleConfig): MirrorModule = QrModule(config)
    override fun defaultConfig() = ModuleConfig(
        module = "qr",
        position = "bottom_left",
        refreshIntervalMs = 0,
        config = mapOf("label" to "Scan to configure")
    )
}