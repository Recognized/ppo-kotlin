package crawler

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.response.HttpResponse
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.jvm.javaio.copyTo
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

private const val READ_LIMIT = 30000L

class UnitTests {

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
        null
    }

    private fun externalUnfurlTest(
        url: String,
        expectedMeta: () -> HtmlMetadata?
    ) {
        val cached = File("src/test/resources/${url.substringAfter("//").replace('/', '_')}")
        val unfurl = if (cached.exists()) {
            runBlocking {
                val result: Pair<HtmlMetaParser, HtmlMetadata>? = run {
                    for (parser in HtmlMetaParser.Parsers) {
                        val data = parser.getMetadata(cached, url)
                        if (data != null) {
                            return@run parser to data
                        }
                    }
                    null
                }
                println("Using: ${result?.first}")
                result?.second
            }
        } else {
            runBlocking {
                val client = HttpClient()
                cached.outputStream().use { out ->
                    client.get<HttpResponse>(url) {
                        header("Range", "bytes:0-${READ_LIMIT}")
                    }.use {
                        it.receive<ByteReadChannel>().copyTo(out, limit = READ_LIMIT)
                    }
                }
                val result: Pair<HtmlMetaParser, HtmlMetadata>? = run {
                    for (parser in HtmlMetaParser.Parsers) {
                        val data = parser.getMetadata(client, READ_LIMIT.toInt(), url)
                        if (data != null) {
                            return@run parser to data
                        }
                    }
                    null
                }
                println("Using: ${result?.first}")
                result?.second
            }
        }
        val meta = expectedMeta()
        if (meta != unfurl) {

        }
        Assertions.assertEquals(meta, unfurl)
    }
}

