package org.speculum.modules.news

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NewsProviderTest {

    private val rss = """
        <?xml version="1.0"?>
        <rss><channel>
          <title>My Feed</title>
          <item><title>First &amp; News</title><pubDate>Wed, 04 Jun 2025 10:00:00 +0000</pubDate></item>
          <item><title><![CDATA[Second]]></title></item>
        </channel></rss>
    """.trimIndent()

    @Test
    fun parsesItemsAndDecodesEntities() {
        val items = NewsProvider().parse(Feed("", "url"), rss)
        assertEquals(2, items.size)
        assertEquals("First & News", items[0].title)   // &amp; decoded
        assertEquals("Second", items[1].title)          // CDATA stripped
    }

    @Test
    fun channelTitleUsedWhenFeedTitleBlank() {
        val items = NewsProvider().parse(Feed("", "url"), rss)
        assertEquals("My Feed", items[0].sourceTitle)
    }

    @Test
    fun feedTitleOverridesChannel() {
        val items = NewsProvider().parse(Feed("Override", "url"), rss)
        assertEquals("Override", items[0].sourceTitle)
    }
}