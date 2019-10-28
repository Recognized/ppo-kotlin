package lab2

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import task2.AppEngineImpl

class UnitTests {
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