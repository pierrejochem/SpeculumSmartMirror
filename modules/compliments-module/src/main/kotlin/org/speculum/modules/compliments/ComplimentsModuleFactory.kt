package org.speculum.modules.compliments

import org.speculum.config.ModuleConfig
import org.speculum.config.SettingSpec
import org.speculum.config.SettingType
import org.speculum.core.MirrorModule
import org.speculum.core.ModuleFactory

class ComplimentsModuleFactory : ModuleFactory {
    override val name = "compliments"
    override fun create(config: ModuleConfig): MirrorModule = ComplimentsModule(config)
    override fun settingsSchema() = listOf(
        SettingSpec(
            key = "compliments", label = "Compliments", type = SettingType.CUSTOM,
            editor = "compliments",
            help = "Text pools per time of day, plus optional date-specific entries.",
        ),
        SettingSpec(
            key = "updateInterval", label = "Rotation (s)", type = SettingType.INT,
            default = "30", min = 1, max = 3600,
            help = "Seconds each compliment stays on screen.",
        ),
        SettingSpec(key = "random", label = "Random order", type = SettingType.BOOL, default = "true"),
        SettingSpec(
            key = "remoteFile", label = "Remote file", type = SettingType.URL, default = "",
            help = "Optional URL serving the same JSON as the editor above; overrides it when set.",
        ),
        SettingSpec(
            key = "fadeSpeed", label = "Fade (ms)", type = SettingType.INT,
            default = "4000", min = 0, max = 60000, step = 500, advanced = true,
        ),
        SettingSpec(
            key = "morningStartTime", label = "Morning starts (h)", type = SettingType.INT,
            default = "3", min = 0, max = 23, advanced = true,
        ),
        SettingSpec(
            key = "morningEndTime", label = "Morning ends (h)", type = SettingType.INT,
            default = "12", min = 0, max = 23, advanced = true,
        ),
        SettingSpec(
            key = "afternoonStartTime", label = "Afternoon starts (h)", type = SettingType.INT,
            default = "12", min = 0, max = 23, advanced = true,
        ),
        SettingSpec(
            key = "afternoonEndTime", label = "Afternoon ends (h)", type = SettingType.INT,
            default = "17", min = 0, max = 23, advanced = true,
        ),
        SettingSpec(
            key = "specialDayUnique", label = "Date entries only", type = SettingType.BOOL,
            default = "false",
            help = "On a matching date, show only that date's compliments.", advanced = true,
        ),
    )

    override fun defaultConfig() = ModuleConfig(
        module = "compliments",
        position = "lower_third",
        refreshIntervalMs = 0,
        config = mapOf("updateInterval" to "30")
    )
}