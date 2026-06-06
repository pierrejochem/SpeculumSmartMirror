package org.speculum.core

import org.speculum.config.ModuleConfig

/**
 * Resolves a config `module` name to an implementation. All modules now ship
 * as external JARs in the `modules/` folder and register themselves at runtime
 * via the [ModuleFactory] SPI (see [discoverPluginFactories]). Add built-in
 * factories to [builtins] if you ever want a module compiled into the app.
 */
object ModuleRegistry {
    private val builtins: Map<String, (ModuleConfig) -> MirrorModule> = emptyMap()

    private val plugins = mutableMapOf<String, ModuleFactory>()

    /** Register an externally-loaded module factory (last one wins). */
    fun register(factory: ModuleFactory) { plugins[factory.name] = factory }

    fun create(config: ModuleConfig): MirrorModule? =
        plugins[config.module]?.create(config)
            ?: builtins[config.module]?.invoke(config)

    val available: Set<String> get() = builtins.keys + plugins.keys
}