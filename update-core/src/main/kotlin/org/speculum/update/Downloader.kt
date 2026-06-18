package org.speculum.update

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import java.io.File

/**
 * Streams release assets to disk. Streaming (not `body<ByteArray>()`) is
 * mandatory: packages bundle a full JRE (~100 MB) and the mirror JVM runs with
 * `-Xmx160m`, so the file must never be buffered whole in memory.
 */
class Downloader(private val client: HttpClient = GitHubReleaseProvider.defaultClient()) {

    /** Downloads [url] to [dest], reporting (bytesRead, totalBytesOrNull). */
    suspend fun download(url: String, dest: File, onProgress: (Long, Long?) -> Unit = { _, _ -> }) {
        client.prepareGet(url) { header("User-Agent", "Speculum-UpdateNotifier") }.execute { response ->
            val total = response.contentLength()
            val channel = response.bodyAsChannel()
            var read = 0L
            dest.outputStream().buffered().use { out ->
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(64 * 1024L)
                    val bytes = packet.readByteArray()
                    if (bytes.isNotEmpty()) {
                        out.write(bytes)
                        read += bytes.size
                        onProgress(read, total)
                    }
                }
            }
        }
    }
}
