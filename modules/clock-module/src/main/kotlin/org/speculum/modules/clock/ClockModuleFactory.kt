package org.speculum.modules.clock

import org.speculum.config.ModuleConfig
import org.speculum.config.SettingSpec
import org.speculum.config.SettingType
import org.speculum.core.MirrorModule
import org.speculum.core.ModuleFactory

class ClockModuleFactory : ModuleFactory {
    override val name = "clock"
    override val order = 0
    override fun create(config: ModuleConfig): MirrorModule = ClockModule(config)
    override fun settingsSchema() = listOf(
        SettingSpec(
            key = "displayType", label = "Display", type = SettingType.ENUM,
            default = "digital", options = listOf("digital", "analog", "both"),
            help = "Which clock faces to show.",
        ),
        SettingSpec(
            key = "timeFormat", label = "Time format", type = SettingType.ENUM,
            default = "24", options = listOf("24", "12"),
            help = "24-hour or 12-hour clock.",
        ),
        SettingSpec(key = "displaySeconds", label = "Show seconds", type = SettingType.BOOL, default = "true"),
        SettingSpec(key = "showDate", label = "Show date", type = SettingType.BOOL, default = "true"),
        SettingSpec(key = "showWeek", label = "Show week number", type = SettingType.BOOL, default = "false"),
        SettingSpec(
            key = "clockBold", label = "Bold time", type = SettingType.BOOL, default = "false",
            advanced = true,
        ),
        SettingSpec(
            key = "showPeriod", label = "Show AM/PM", type = SettingType.BOOL, default = "true",
            help = "12-hour format only.", advanced = true,
        ),
        SettingSpec(
            key = "showPeriodUpper", label = "Uppercase AM/PM", type = SettingType.BOOL, default = "false",
            advanced = true,
        ),
        SettingSpec(
            key = "analogSize", label = "Analog size (dp)", type = SettingType.INT,
            default = "200", min = 80, max = 600, step = 10,
            help = "Diameter of the analog face.", advanced = true,
        ),
        SettingSpec(
            key = "analogPlacement", label = "Analog placement", type = SettingType.ENUM,
            default = "bottom", options = listOf("top", "bottom", "left", "right"),
            help = "Where the analog face sits relative to the digital time.", advanced = true,
        ),
    )

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