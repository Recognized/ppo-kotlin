package crawler

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.response.HttpResponse
import io.ktor.http.charset
import io.ktor.http.contentType
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.jvm.javaio.copyTo
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.nio.file.Files
import java.nio.file.Path

private const val BUFFER_SIZE = 8192

object JsoupMetaParser : HtmlMetaParser {

    override suspend fun cacheResponse(client: HttpClient, limit: Int, url: String, file: Path) {
        Files.newOutputStream(file).use { out ->
            client.get<HttpResponse>(url) {
                header("Range", "bytes:0-${limit}")
            }.use {
                it.receive<ByteReadChannel>().copyTo(out, limit.toLong())
            }
        }
    }

    override suspend fun getMetadata(client: HttpClient, limit: Int, url: String): HtmlMetadata? {
        return UnfurlCrawler.createUnfurlFromMeta(
            parseMeta(client.readResponse(limit, url), url)
        ).firstOrNull()
    }

    override fun getMetadata(file: Path, url: String): HtmlMetadata? {
        return UnfurlCrawler.createUnfurlFromMeta(
            Files.newBufferedReader(file).use {
                parseMeta(it.readText(), url)
            }
        ).firstOrNull()
    }

    private fun parseMeta(html: String, baseUri: String): MetaAttributes {
        val document = Jsoup.parse(html, baseUri, Parser.htmlParser())
        val attributes = mutableListOf<Map<String, String>>()
        document.head().allElements.forEach { tag ->
            val attrs = mutableMapOf<String, String>()
            when (tag.tagName()) {
                "meta" -> {
                    tag.attributes().forEach {
                        attrs[it.key] = it.value
                    }
                }
                "title" -> {
                    attrs["title"] = tag.ownText()
                }
            }
            attributes += attrs
        }
        return attributes
    }

    override fun toString(): String = "JsoupMetaParser"
}

suspend fun HttpClient.readResponse(limit: Int, url: String): String {
    val call = get<HttpResponse>(url) {
        header("Range", "bytes:0-$limit")
    }
    val charset = call.contentType()?.charset() ?: Charsets.UTF_8
    val decoder = charset.newDecoder()
    decoder.onUnmappableCharacter(CodingErrorAction.IGNORE)
    decoder.onMalformedInput(CodingErrorAction.IGNORE)
    val content = call.content
    val buffer = ByteBuffer.allocate(BUFFER_SIZE)
    val out = CharBuffer.allocate(limit)
    while (!call.content.isClosedForRead) {
        val count = content.readAvailable(buffer)
        if (count == 0) {
            break
        }
        buffer.flip()
        val error = decoder.decode(buffer, out, false)
        if (buffer.hasRemaining()) {
            val rest = ByteArray(buffer.remaining())
            buffer.get(rest)
            buffer.clear()
            buffer.put(rest)
        } else {
            buffer.clear()
        }
        if (error.isOverflow) {
            break
        }
    }
    out.flip()
    return out.toString()
}