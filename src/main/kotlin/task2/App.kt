package task2

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import java.time.Duration
import java.time.Instant
import java.util.*

interface AppEngine {
    suspend fun search(tag: String, hours: Int): List<Int>
}

class AppEngineImpl(val auth: AppAuth, val client: HttpClient) : AppEngine {

    private suspend inline fun HttpClient.getJson(url: String, builder: HttpRequestBuilder.() -> Unit): JsonElement {
        return Json(JsonConfiguration.Stable).parse(JsonElementSerializer, this.get(url, builder))
    }

    override suspend fun search(tag: String, hours: Int): List<Int> {
        if (hours !in 1..24) {
            error("Invalid hours: $hours, must be in 1..24")
        }
        val now = System.currentTimeMillis() / 1000L
        val hour = 60L * 60
        println(now)
        val response = client.getJson("https://api.vk.com/method/newsfeed.search") {
            parameter("access_token", auth.serviceKey)
            parameter("client_secret", auth.secretKey)
            parameter("v", auth.version)
            parameter("q", tag)
            parameter("count", 100)
            parameter("start_time", now - hour * hours)
        }
        println("[client]: Response: \"$response\"")
        val data = response.jsonObject.getObject("response").getArray("items").map {
            (now - it.jsonObject.getPrimitive("date").long) / hour
        }
        println("[client]: $data")
        val count = data.filter { it in 0..23 }.groupBy { it }.mapValues { it.value.size }
        return (0 until hours).map { count[it.toLong()] ?: 0 }
    }
}