package org.speculum.modules.news

import org.speculum.config.ModuleConfig
import org.speculum.config.SettingSpec
import org.speculum.config.SettingType
import org.speculum.core.MirrorModule
import org.speculum.core.ModuleFactory

class NewsFeedModuleFactory : ModuleFactory {
    override val name = "newsfeed"
    override fun create(config: ModuleConfig): MirrorModule = NewsFeedModule(config)
    override fun settingsSchema() = listOf(
        SettingSpec(key = "title", label = "Source name", default = "New York Times"),
        SettingSpec(
            key = "url", label = "Feed URL", type = SettingType.URL,
            default = "https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml",
            help = "RSS or Atom feed to read headlines from.",
        ),
        SettingSpec(
            key = "updateInterval", label = "Headline rotation (s)", type = SettingType.INT,
            default = "10", min = 3, max = 600,
            help = "Seconds each headline stays on screen.",
        ),
        SettingSpec(key = "showSourceTitle", label = "Show source name", type = SettingType.BOOL, default = "true"),
        SettingSpec(key = "showPublishDate", label = "Show publish date", type = SettingType.BOOL, default = "true"),
    )

    override fun defaultConfig() = ModuleConfig(
        module = "newsfeed",
        position = "bottom_bar",
        refreshIntervalMs = 300_000,
        config = mapOf(
            "title" to "New York Times",
            "url" to "https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml",
            "updateInterval" to "10",
            "showSourceTitle" to "true",
            "showPublishDate" to "true",
        )
    )
}