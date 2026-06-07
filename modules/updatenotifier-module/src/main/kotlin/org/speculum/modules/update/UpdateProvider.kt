package org.speculum.modules.update

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Newest published release of a GitHub repo. */
data class UpdateInfo(val version: String, val url: String)

/**
 * Reads `…/releases/latest` from the GitHub REST API (unauthenticated — 60
 * req/h, ample for a multi-hour refresh). A User-Agent header is required by
 * GitHub. Any failure (no releases, offline, rate-limited) maps to null so the
 * module simply shows nothing.
 */
class GitHubReleaseProvider(private val repo: String) {

    private val client = HttpClient {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    suspend fun latest(): Release? = runCatching {
        client.get("https://api.github.com/repos/$repo/releases/latest") {
            header("User-Agent", "Speculum-UpdateNotifier")
            header("Accept", "application/vnd.github+json")
        }.body<Release>()
    }.getOrNull()
}

@Serializable
data class Release(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
)

/**
 * True if [latest] is a strictly higher version than [current]. Leading "v" is
 * ignored and versions are compared component-by-component as integers
 * (e.g. "1.2.0" > "1.1.9"); a pre-release suffix is dropped at the first dash.
 */
fun isNewer(latest: String, current: String): Boolean {
    fun parts(v: String): List<Int> =
        v.trim().removePrefix("v").removePrefix("V")
            .substringBefore('-')
            .split('.')
            .mapNotNull { it.toIntOrNull() }
    val a = parts(latest)
    val b = parts(current)
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}