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
            AppEngineImpl(
                MockAuth,
                dateMockedClient((0..23).map { it * 60 * 60 + 30 }),
                24 * 60 * 60
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

    @Test
    fun `test hours must be positive`() {
        runBlocking {
            Assertions.assertEquals(1, AppEngineImpl(MockAuth, mockedClient).search("response1", 1).size)
        }

        assertThrows<Throwable> {
            runBlocking {
                AppEngineImpl(MockAuth, mockedClient).search("response1", 0)
            }
        }
    }

    @Test
    fun `test hours cannot be greater 24`() {
        runBlocking {
            Assertions.assertEquals(24, AppEngineImpl(MockAuth, mockedClient).search("response1", 24).size)
        }

        assertThrows<Throwable> {
            runBlocking {
                AppEngineImpl(MockAuth, mockedClient).search("response1", 25)
            }
        }
    }
}