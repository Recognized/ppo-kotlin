package crawler

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.json.*
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.Path

private val log = Logger(OEmbed.toString())

object OEmbed : HtmlMetaParser {
    private val endpointProviders = listOf(
        YoutubeOEmbedProvider
    )

    override suspend fun cacheResponse(client: HttpClient, limit: Int, url: String, file: Path) {
        val provider = getProvider(url) ?: return
        val response = client.get<String>(provider.endpoint) {
            parameter("url", url)
        }
        Files.newBufferedWriter(file).use {
            it.write(response)
        }
    }

    override suspend fun getMetadata(client: HttpClient, limit: Int, url: String): HtmlMetadata? {
        val provider = getProvider(url) ?: run {
            log.info { "Provider not found for $url" }
            return null
        }
        log.info { "Found provider $provider" }
        return try {
            val response = client.get<String>(provider.endpoint) {
                parameter("url", url)
            }
            processResponse(provider, response)
        } catch (ex: Throwable) {
            ex.printStackTrace()
            return null
        }
    }

    override fun getMetadata(file: Path, url: String): HtmlMetadata? {
        val provider = getProvider(url) ?: return null
        val response = Files.newBufferedReader(file).readText()
        return processResponse(provider, response)
    }

    private fun processResponse(provider: OEmbedProviderProvider, response: String): HtmlMetadata? {
        return try {
            val json = Json(JsonConfiguration.Stable).parse(JsonObjectSerializer, response)
            provider.parseResponse(json)
        } catch (ex: Throwable) {
            log.info { "Fail: ${ex.message}" }
            null
        }
    }

    private fun getProvider(baseUrl: String): OEmbedProviderProvider? {
        val host = try {
            URI(baseUrl).host.removePrefix("www.")
        } catch (ex: URISyntaxException) {
            log.info { "Fail: ${ex.message}" }
            return null
        }
        log.info { "Using host: $host" }
        return endpointProviders.firstOrNull {
            it.accept(host, baseUrl)
        }
    }

    override fun toString(): String = "oEmbed"
}

interface OEmbedProviderProvider {
    val endpoint: String

    fun accept(host: String, url: String): Boolean

    fun parseResponse(json: JsonElement): HtmlMetadata?
}

object YoutubeOEmbedProvider : OEmbedProviderProvider {
    private val urlRegexes = listOf(
        "https://.*\\.youtube\\.com/watch.*".toRegex(),
        "https://.*\\.youtube\\.com/v/.*".toRegex(),
        "https://youtu\\.be/.*".toRegex()
    )

    override val endpoint: String get() = "https://www.youtube.com/oembed"

    override fun accept(host: String, url: String): Boolean {
        return host.removePrefix("www.") == "youtube.com" && urlRegexes.any { it.matches(url) }
    }

    override fun parseResponse(json: JsonElement): HtmlMetadata? {
        val obj = json.jsonObject
        return try {
            YoutubeOEmbed(
                thumbnailUrl = obj.getString("thumbnail_url"),
                thumbnailWidth = obj.getPrimitive("thumbnail_width").int,
                thumbnailHeight = obj.getPrimitive("thumbnail_height").int,
                authorName = obj.getString("author_name"),
                authorUrl = obj.getString("author_url"),
                title = obj.getString("title"),
                type = obj.getString("type"),
                version = obj.getString("version"),
                html = obj.getString("html")
            )
        } catch (ex: Throwable) {
            log.info { "Fail: ${ex.message}" }
            null
        }
    }

    override fun toString(): String = "Youtube"
}

private fun JsonObject.getString(key: String) = getPrimitive(key).content

data class YoutubeOEmbed(
    val thumbnailUrl: String,
    val thumbnailWidth: Int,
    val thumbnailHeight: Int,
    val authorName: String,
    val authorUrl: String,
    val title: String,
    val html: String,
    val type: String,
    val version: String
) : HtmlMetadata