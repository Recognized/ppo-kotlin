package crawler

import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.nio.file.Files
import java.nio.file.Paths

private const val READ_LIMIT = 30000

class UnitTests {
    private val log = Logger("TEST")

    @Test
    fun `test google`() = externalUnfurlTest("https://www.google.com") {
        GeneralPageHtmlMetadata(
            title = "Google",
            description = "Поиск информации в интернете: веб страницы, картинки, видео и многое другое.",
            image = null
        )
    }

    @Test
    fun `test twitter`() = externalUnfurlTest("https://www.twitter.com") {
        GeneralPageHtmlMetadata(
            title = "Твиттер. Здесь говорят о том, что происходит.",
            description = "Главные новости, развлечения, спорт и политика — узнавайте обо всем и читайте комментарии в реальном времени.",
            image = null
        )
    }

    @Test
    fun `test facebook`() = externalUnfurlTest("http://www.facebook.com/") {
        OpenGraph(
            url = "https://www.facebook.com/",
            title = "Facebook — Выполните вход или зарегистрируйтесь",
            image = OpenGraphImage(
                url = "https://www.facebook.com/images/fb_icon_325x325.png",
                mime = null,
                width = null,
                height = null,
                alt = null
            ),
            description = null,
            determiner = null,
            siteName = "Facebook",
            video = null,
            audio = null
        )
    }

    @Test
    fun `test youtube video`() = externalUnfurlTest("https://www.youtube.com/watch?v=Iydpa_gPdes") {
        YoutubeOEmbed(
            thumbnailUrl = "https://i.ytimg.com/vi/Iydpa_gPdes/hqdefault.jpg",
            thumbnailWidth = 480,
            thumbnailHeight = 360,
            authorName = "TechLead",
            authorUrl = "https://www.youtube.com/channel/UC4xKdmAXFh4ACyhpiQ_3qBw",
            title = "What is a 10x Engineer (feat. ex-Google Tech Lead) #10xengineer",
            html = "<iframe width=\"480\" height=\"270\" src=\"https://www.youtube.com/embed/Iydpa_gPdes?feature=oembed\" frameborder=\"0\" allow=\"accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture\" allowfullscreen></iframe>",
            type = "video",
            version = "1.0"
        )
    }

    private fun externalUnfurlTest(
        url: String,
        expectedMeta: () -> HtmlMetadata?
    ) {
        val client = HttpClient()
        val unfurls = HtmlMetaParser.Parsers.map { parser ->
            val parserDir = Paths.get("src", "test", "resources", parser.toString())
            Files.createDirectories(parserDir)
            val cached = parserDir.resolve(url.substringAfter("//").replace('/', '_'))
            val unfurl = runBlocking {
                if (Files.exists(cached)) {
                    log.info { "Cache exists, reading $cached" }
                    parser.getMetadata(cached, url)
                } else {
                    log.info { "Caching response" }
                    parser.cacheResponse(client, READ_LIMIT, url, cached)
                    log.info { "Get metadata" }
                    parser.getMetadata(client, READ_LIMIT, url)
                }
            }
            parser to unfurl
        }.toMap()
        val meta = expectedMeta()
        if (unfurls.values.none { it == meta }) {
            fail {
                "Expected: $meta, but got: $unfurls"
            }
        }
    }
}

