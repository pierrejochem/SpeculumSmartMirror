package org.speculum.modules.qr

import org.speculum.config.ModuleConfig
import org.speculum.config.SettingSpec
import org.speculum.config.SettingType
import org.speculum.core.MirrorModule
import org.speculum.core.ModuleFactory

class QrModuleFactory : ModuleFactory {
    override val name = "qr"
    override fun create(config: ModuleConfig): MirrorModule = QrModule(config)
    override fun settingsSchema() = listOf(
        SettingSpec(key = "label", label = "Caption", default = "Scan to configure"),
        SettingSpec(
            key = "url", label = "URL override", type = SettingType.URL, default = "",
            placeholder = "built from IP and port",
            help = "Leave blank to point the code at this mirror's config console.",
            preview = listOf("{url}", "http://{ip|<auto LAN IP>}:{port|8080}"),
        ),
        SettingSpec(
            key = "ip", label = "Mirror IP", type = SettingType.IP, default = "",
            help = "Blank auto-detects the LAN address. Ignored when a URL override is set.",
        ),
        SettingSpec(
            key = "port", label = "Port", type = SettingType.INT,
            default = "8080", min = 1, max = 65535,
            help = "Port the config console listens on.",
        ),
        SettingSpec(
            key = "size", label = "Size (dp)", type = SettingType.INT,
            default = "110", min = 60, max = 400, step = 10,
        ),
    )

    override fun defaultConfig() = ModuleConfig(
        module = "qr",
        position = "bottom_left",
        refreshIntervalMs = 0,
        config = mapOf("label" to "Scan to configure")
    )
}