package org.speculum.modules.news

import org.speculum.config.ModuleConfig
import org.speculum.core.MirrorModule
import org.speculum.core.ModuleFactory

class NewsFeedModuleFactory : ModuleFactory {
    override val name = "newsfeed"
    override fun create(config: ModuleConfig): MirrorModule = NewsFeedModule(config)
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