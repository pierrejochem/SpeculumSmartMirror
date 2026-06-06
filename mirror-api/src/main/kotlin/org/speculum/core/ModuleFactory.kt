package org.speculum.core

import org.speculum.config.ModuleConfig

/**
 * Service-provider interface for pluggable modules. A module packaged as a
 * separate JAR implements this and declares it in
 * `META-INF/services/org.speculum.core.ModuleFactory`. The desktop app
 * discovers these via reflection (ServiceLoader) from the `modules/` folder,
 * registers them, and auto-adds `defaultConfig()` so the module shows up
 * without editing the bundled config.
 */
interface ModuleFactory {
    /** The config `module` name this factory handles (e.g. "example"). */
    val name: String

    /** Build a module instance for the given config entry. */
    fun create(config: ModuleConfig): MirrorModule

    /**
     * Optional default placement, auto-added when no config entry already
     * names this module. Return null to require an explicit config entry.
     */
    fun defaultConfig(): ModuleConfig? = null

    /**
     * Stacking order within a region (lower = higher/earlier). Used to keep
     * a deterministic layout when modules are discovered from JARs in an
     * unspecified order (e.g. clock above calendar in top_left).
     */
    val order: Int get() = 0
}