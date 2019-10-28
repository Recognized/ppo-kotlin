package lab2

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import task2.AppEngineImpl
import java.io.File

class MockTests {
    private val timeSnapshot = 1572221880L

    @Test
    fun `test mock tag1`() {
        Assertions.assertEquals(
            listOf(10, 0, 0, 0, 0),
            runBlocking {
                AppEngineImpl(MockAuth, mockedClient, timeSnapshot).search("response1", 5)
            }
        )
    }

    @Test
    fun `test mock tag2`() {
        Assertions.assertEquals(
            listOf(1, 0, 2, 2, 11, 10, 8, 19, 13, 7, 12, 10, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            runBlocking {
                AppEngineImpl(MockAuth, mockedClient, timeSnapshot).search("итмо", 24)
            }
        )
    }

    @Test
    fun `test one article per hour`() {
        Assertions.assertEquals((1..24).map { 1 }, runBlocking {
            MockEngine(
                MockAuth,
                mockedClient,
                24 * 60 * 60,
                (0..23).map { it * 60 * 60L + 30 }
            ).search("onePerHour", 24)
        })
    }

    @Test
    fun `test all long time ago`() {
        Assertions.assertEquals((1..24).map { 0 }, runBlocking {
            AppEngineImpl(
                MockAuth,
                dateMockedClient((0..23).map { 0 }),
                1_000_000_000
            ).search("onePerHour", 24)
        })
    }
}