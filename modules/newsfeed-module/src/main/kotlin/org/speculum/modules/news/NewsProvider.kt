package org.speculum.modules.news

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText

/** One parsed RSS entry, mirroring MagicMirror newsfeed's item shape. */
data class NewsItem(
    val title: String,
    val sourceTitle: String,
    val pubDate: String,
)

/** A configured feed, equivalent to one entry in newsfeed's `feeds: [{title, url}]`. */
data class Feed(val title: String, val url: String)

/**
 * Fetches and parses RSS feeds, like the node_helper behind MagicMirror's
 * `newsfeed` module. Uses a lightweight regex parser so it stays multiplatform
 * (no XML library dependency).
 */
class NewsProvider {

    private val client by lazy { HttpClient() }

    suspend fun fetch(feeds: List<Feed>): List<NewsItem> =
        feeds.flatMap { feed ->
            runCatching { parse(feed, client.get(feed.url) {
                header("User-Agent", "Speculum/1.0")
            }.bodyAsText()) }.getOrDefault(emptyList())
        }

    internal fun parse(feed: Feed, xml: String): List<NewsItem> {
        // Channel title is the first <title> before any <item>/<entry>.
        val channelTitle = ITEM.find(xml)?.range?.first
            ?.let { firstItem -> TITLE.find(xml.substring(0, firstItem)) }
            ?.let { clean(it.groupValues[1]) }
        val sourceName = feed.title.ifBlank { channelTitle.orEmpty() }

        return ITEM.findAll(xml).map { m ->
            val block = m.groupValues[1]
            NewsItem(
                title = TITLE.find(block)?.let { clean(it.groupValues[1]) }.orEmpty(),
                sourceTitle = sourceName,
                pubDate = PUBDATE.find(block)?.let { clean(it.groupValues[1]) }.orEmpty(),
            )
        }.filter { it.title.isNotBlank() }.toList()
    }

    private fun clean(raw: String): String =
        raw.replace("<![CDATA[", "").replace("]]>", "")
            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'")
            .trim()

    private companion object {
        // RSS uses <item>, Atom uses <entry>; match either.
        val ITEM = Regex("<(?:item|entry)\\b[^>]*>(.*?)</(?:item|entry)>", RegexOption.DOT_MATCHES_ALL)
        val TITLE = Regex("<title\\b[^>]*>(.*?)</title>", RegexOption.DOT_MATCHES_ALL)
        val PUBDATE = Regex("<(?:pubDate|published|updated)\\b[^>]*>(.*?)</(?:pubDate|published|updated)>",
            RegexOption.DOT_MATCHES_ALL)
    }
}