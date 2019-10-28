package lab2

import io.ktor.client.HttpClient
import task2.AppAuth
import task2.AppEngineImpl

class MockEngine(auth: AppAuth, client: HttpClient, now: Long, val dates: List<Long>) :
    AppEngineImpl(auth, client, now) {
    override suspend fun getDates(tag: String, hours: Int): List<Long> {
        return dates
    }
}