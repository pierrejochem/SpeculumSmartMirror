package org.speculum.modules.calendar

import org.speculum.config.ModuleConfig
import org.speculum.config.SettingSpec
import org.speculum.config.SettingType
import org.speculum.core.MirrorModule
import org.speculum.core.ModuleFactory

class CalendarModuleFactory : ModuleFactory {
    override val name = "calendar"
    override val order = 1  // below the clock in top_left
    override fun create(config: ModuleConfig): MirrorModule = CalendarModule(config)
    override fun settingsSchema() = listOf(
        SettingSpec(key = "header", label = "Header", default = "US Holidays"),
        SettingSpec(
            key = "url", label = "Calendar URL", type = SettingType.URL,
            default = "https://ics.calendarlabs.com/76/mm3137/US_Holidays.ics",
            help = "Public iCalendar (.ics) feed to read events from.",
        ),
        SettingSpec(
            key = "maximumEntries", label = "Max entries", type = SettingType.INT,
            default = "10", min = 1, max = 50,
            help = "How many upcoming events to list.",
        ),
        SettingSpec(
            key = "maximumNumberOfDays", label = "Days ahead", type = SettingType.INT,
            default = "365", min = 1, max = 3650,
            help = "Ignore events further out than this.",
        ),
        SettingSpec(
            key = "symbol", label = "Icon", default = "calendar-check",
            help = "Glyph drawn beside each event.", advanced = true,
        ),
    )

    override fun defaultConfig() = ModuleConfig(
        module = "calendar",
        position = "top_left",
        refreshIntervalMs = 600_000,
        config = mapOf(
            "header" to "US Holidays",
            "symbol" to "calendar-check",
            "url" to "https://ics.calendarlabs.com/76/mm3137/US_Holidays.ics",
            "maximumEntries" to "6",
            "maximumNumberOfDays" to "365",
        )
    )
}