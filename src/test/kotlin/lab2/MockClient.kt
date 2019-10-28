package lab2

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.headersOf
import org.apache.http.entity.ContentType

val mockedClient = HttpClient(MockEngine) {
    engine {
        addHandler { request ->
            val tag = request.url.parameters["q"]
            val responseHeaders = headersOf("Content-Type" to listOf(ContentType.APPLICATION_JSON.toString()))
            respond(this::class.java.classLoader.getResource("$tag.json")!!.readText(), headers = responseHeaders)
        }
    }
}