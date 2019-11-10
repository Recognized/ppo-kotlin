package crawler

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.response.HttpResponse
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.jvm.javaio.copyTo
import kotlinx.coroutines.runBlocking
import org.apache.commons.text.StringEscapeUtils
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

private val emptyMeta: MetaAttributes = listOf()

interface HtmlMetaParser {

    suspend fun cacheResponse(client: HttpClient, limit: Int, url: String, file: Path)
    suspend fun getMetadata(client: HttpClient, limit: Int, url: String): HtmlMetadata?
    fun getMetadata(file: Path, url: String): HtmlMetadata?

    companion object {
        val Parsers = listOf(OEmbed, JsoupMetaParser)
    }
}

object HandHtmlMetaParser : HtmlMetaParser {
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
        return client.get<HttpResponse>(url) {
            header("Range", "bytes:0-${limit}")
        }.use {
            UnfurlCrawler.createUnfurlFromMeta(
                HandHtmlMetaParserImpl(TextReadChannelImpl(limit.toLong(), it.receive())).consumeDocument()
            ).firstOrNull()
        }
    }

    override fun getMetadata(file: Path, url: String): HtmlMetadata? {
        return FileTextReadChannel(file.toString()).use {
            UnfurlCrawler.createUnfurlFromMeta(
                runBlocking {
                    HandHtmlMetaParserImpl(it).consumeDocument()
                }
            ).firstOrNull()
        }
    }

    override fun toString(): String = "HandHtmlMetaParser"
}

private class HandHtmlMetaParserImpl(private val channel: TextReadChannel) {
    private var currentSymbol: Int = -1

    suspend fun consumeDocument(): MetaAttributes {
        currentSymbol = channel.next()
        var context = emptyMeta
        while (currentSymbol >= 0) {
            context = context + consumeTagStart()
        }
        return context
    }

    private suspend fun consumeTag(): MetaAttributes {
        if (currentSymbol < 0) {
            return emptyMeta
        }
        consumeJunk { it == '<' }
        currentSymbol = channel.next()
        consumeWs()
        return if (currentSymbol.toChar() == '/') {
            consumeTagEndReal()
        } else {
            consumeTagStart()
        }
    }

    private suspend fun consumeTagStart(): MetaAttributes {
        if (currentSymbol < 0) {
            return emptyMeta
        }
        consumeWs()
        val tagName = consumeWord().toLowerCase()
        when {
            tagName.isNotEmpty() -> {
                if (tagName == "body") {
                    currentSymbol = -1
                    return emptyMeta
                }
                val data = if (tagName == "meta") {
                    consumeAttributes()
                } else {
                    consumeAttributes()
                    emptyList()
                }
                if (currentSymbol < 0 || currentSymbol.toChar() == '/') {
                    consumeWs()
                    if (currentSymbol.toChar() == '>') {
                        currentSymbol = channel.next()
                    }
                    return data
                }
                if (currentSymbol.toChar() == '>') {
                    currentSymbol = channel.next()
                }
                val inner = if (tagName == "title") {
                    if (currentSymbol.toChar() == '>') {
                        currentSymbol = channel.next()
                    }
                    listOf(mapOf(tagName to consumeTagContents(200)))
                } else {
                    consumeTag()
                }
                return consumeTagEnd() + inner + data
            }
            currentSymbol >= 0 -> return consumeTag()
            else -> return emptyMeta
        }
    }

    private suspend fun consumeTagContents(limit: Int): String {
        return buildString {
            var iter = 0
            while (true) {
                if (currentSymbol < 0) {
                    break
                }
                val char = currentSymbol.toChar()
                if (char == '<') {
                    break
                }
                if (iter < limit) {
                    append(char)
                }
                currentSymbol = channel.next()
                iter++
            }
        }
    }

    private suspend fun consumeTagEnd(): MetaAttributes {
        if (currentSymbol < 0) {
            return emptyMeta
        }
        consumeJunk { it == '<' }
        currentSymbol = channel.next()
        consumeWs()
        if (currentSymbol < 0) {
            return emptyMeta
        }
        return if (currentSymbol.toChar() == '/') {
            currentSymbol = channel.next()
            consumeTagEndReal()
        } else {
            consumeTagStart()
        }
    }

    private suspend fun consumeTagEndReal(): MetaAttributes {
        if (currentSymbol < 0) {
            return emptyMeta
        }
        consumeWs()
        consumeWord()
        consumeJunk { it == '>' || it == '<' }
        if (currentSymbol < 0) {
            return emptyMeta
        }
        if (currentSymbol.toChar() == '>') {
            currentSymbol = channel.next()
        }
        if (currentSymbol.toChar() == '<') {
            return consumeTagStart()
        }
        return emptyMeta
    }

    private suspend fun consumeAttributes(): MetaAttributes {
        val attributes = mutableMapOf<String, String>()
        while (true) {
            if (currentSymbol < 0) break
            val char = currentSymbol.toChar()
            if (char == '/' || char == '>' || char == '<') {
                break
            }
            consumeWs()
            val key = consumeWord(20)
            if (key.isNotEmpty()) {
                consumeWs()
                if (currentSymbol.toChar() == '=') {
                    currentSymbol = channel.next()
                    consumeWs()
                    val value = consumeString()
                    attributes[key] = value
                } else {
                    attributes[key] = "true"
                }
            } else {
                currentSymbol = channel.next()
            }
            consumeWs()
        }
        return listOf(attributes)
    }

    private suspend fun consumeString(max: Int = 200): String {
        if (currentSymbol < 0) {
            return ""
        }
        return if (currentSymbol.toChar() == '"' || currentSymbol.toChar() == '\'') {
            val singleQuote = currentSymbol.toChar() == '\''
            buildString {
                var iter = 0
                while (true) {
                    val next = channel.next()
                    if (next < 0) {
                        return@buildString
                    }
                    val char = next.toChar()
                    if (char == '\'' && singleQuote || char == '"' && !singleQuote) {
                        currentSymbol = channel.next()
                        return@buildString
                    } else if (iter < max * 8) {
                        append(char)
                    }
                    iter++
                }
            }.let {
                StringEscapeUtils.unescapeHtml4(it)
            }
        } else {
            consumeWord(max)
        }
    }

    private suspend fun consumeJunk(stop: (Char) -> Boolean) {
        while (true) {
            val next = currentSymbol
            if (next < 0 || stop(next.toChar())) {
                return
            }
            currentSymbol = channel.next()
        }
    }

    private suspend fun consumeWs() {
        consumeJunk { !it.isWhitespace() }
    }

    private suspend fun consumeWord(limit: Int = 10): String {
        return buildString {
            var iter = 0
            while (true) {
                if (currentSymbol < 0) {
                    return@buildString
                }
                val char = currentSymbol.toChar()
                if (char !in 'a'..'z' && char !in 'A'..'Z' && char != ':') {
                    return@buildString
                }
                if (iter < limit) {
                    append(char)
                }
                currentSymbol = channel.next()
                iter++
            }
        }
    }
}

private class FileTextReadChannel(file: String) : TextReadChannel, Closeable {
    private val reader = File(file).bufferedReader()

    override suspend fun next(): Int {
        return reader.read()
    }

    override fun close() {
        reader.close()
    }
}