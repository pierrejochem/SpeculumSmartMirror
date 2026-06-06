package org.speculum.modules.example

import org.speculum.config.ModuleConfig
import org.speculum.core.MirrorModule
import org.speculum.core.ModuleFactory

/**
 * SPI entry point discovered by the host app via ServiceLoader (declared in
 * META-INF/services/org.speculum.core.ModuleFactory). Provides the module
 * name, a constructor, and a default placement so it appears automatically.
 */
class ExampleModuleFactory : ModuleFactory {
    override val name: String = "example"

    override fun create(config: ModuleConfig): MirrorModule = ExampleModule(config)

    override fun defaultConfig(): ModuleConfig = ModuleConfig(
        module = "example",
        position = "top_center",
        refreshIntervalMs = 3000,
        config = mapOf("greeting" to "Loaded from JAR!", "tickStep" to "2")
    )
}