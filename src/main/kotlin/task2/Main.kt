package task2

import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        println(AppEngineImpl(SecretAuth, HttpClient()).search("итмо", 24))
    }
}

