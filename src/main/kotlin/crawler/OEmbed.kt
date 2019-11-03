package crawler

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.json.*
import java.io.File
import java.net.URI
import java.net.URISyntaxException

object OEmbed : HtmlMetaParser {
    private val endpointProviders = listOf(
        YoutubeOEmbedProvider
    )

    override suspend fun getMetadata(client: HttpClient, limit: Int, url: String): HtmlMetadata? {
        val provider = getProvider(url) ?: return null
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

    override suspend fun getMetadata(file: File, url: String): HtmlMetadata? {
        val provider = getProvider(url) ?: return null
        val response = file.bufferedReader().readText()
        return processResponse(provider, response)
    }

    private fun processResponse(provider: OEmbedProviderProvider, response: String): HtmlMetadata? {
        return try {
            val json = Json(JsonConfiguration.Stable).parse(JsonObjectSerializer, response)
            provider.parseResponse(json)
        } catch (ex: Throwable) {
            println(ex.message)
            null
        }
    }

    fun getProvider(baseUrl: String): OEmbedProviderProvider? {
        val host = try {
            URI(baseUrl).host.removePrefix("www.")
        } catch (ex: URISyntaxException) {
            return null
        }
        return endpointProviders.firstOrNull {
            it.accept(host, baseUrl)
        }
    }
}

interface OEmbedProviderProvider {
    val endpoint: String

    fun accept(host: String, url: String): Boolean

    fun parseResponse(json: JsonElement): HtmlMetadata?
}

object YoutubeOEmbedProvider : OEmbedProviderProvider {
    private val urlRegexes = listOf(
        "https://*.youtube.com/watch*".toRegex(),
        "https://*.youtube.com/v/*".toRegex(),
        "https://youtu.be/*".toRegex()
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
            null
        }
    }
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