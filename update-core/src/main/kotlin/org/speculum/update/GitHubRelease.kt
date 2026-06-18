package org.speculum.update

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

/** One downloadable file attached to a GitHub release. */
@Serializable
data class ReleaseAsset(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = "",
    val size: Long = 0,
)

@Serializable
data class Release(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val assets: List<ReleaseAsset> = emptyList(),
)

/**
 * Reads `…/releases/latest` from the GitHub REST API (unauthenticated — 60
 * req/h, ample for a multi-hour refresh). A User-Agent header is required by
 * GitHub. Any failure (no releases, offline, rate-limited) maps to null so the
 * caller simply shows nothing.
 */
class GitHubReleaseProvider(
    private val repo: String,
    private val client: HttpClient = defaultClient(),
) {
    suspend fun latest(): Release? = runCatching {
        client.get("https://api.github.com/repos/$repo/releases/latest") {
            header("User-Agent", "Speculum-UpdateNotifier")
            header("Accept", "application/vnd.github+json")
        }.body<Release>()
    }.getOrNull()

    companion object {
        fun defaultClient(): HttpClient = HttpClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }
}
