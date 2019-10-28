package lab2

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import task2.AppEngineImpl

class MockTests {

    @Test
    fun `test mock tag1`() {
        Assertions.assertEquals(runBlocking {
            AppEngineImpl(MockAuth, mockedClient).search("response1", 5)
        }, listOf(10, 0, 0, 0, 0))
    }

    @Test
    fun `test mock tag2`() {
        Assertions.assertEquals(runBlocking {
            AppEngineImpl(MockAuth, mockedClient).search("итмо", 24)
        }, listOf(1, 0, 1, 2, 10, 6, 10, 15, 18, 8, 12, 9, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
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