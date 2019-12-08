package task7

import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class EventStatisticsTest {

    @Test
    fun `test no events`() = test {
        millis += 2.h
        assert(it.getAllEventStatistics().isEmpty())
    }

    @Test
    fun `test event expires exactly in one hour`() = test {
        it.incEvent("test")
        millis += 1.h
        assert(it.getAllEventStatistics() == listOf(Statistics("test", 1)))
        millis += 1
        assert(it.getAllEventStatistics() == listOf(Statistics("test", 0)))
    }

    @Test
    fun `test increments sums up`() = test {
        it.incEvent("test")
        assert(it.getEventStatisticsByName("test").times == 1L)
        it.incEvent("test")
        assert(it.getEventStatisticsByName("test").times == 2L)
    }

    @Test
    fun `test statistics is independent`() = test {
        it.incEvent("1")
        millis += 1
        it.incEvent("2")
        assert(it.getEventStatisticsByName("1").times == 1L)
        assert(it.getEventStatisticsByName("2").times == 1L)
        millis += 1.h
        assert(it.getAllEventStatistics().toSet() == setOf(Statistics("2", 1), Statistics("1", 0)))
    }

    @Test
    fun `test last hour statistics`() = test {
        for (i in 1..60) {
            it.incEvent("test")
            millis += 1.m
        }
        // 1 hour spent
        for (i in 1..61) {
            assert(it.getEventStatisticsByName("test").times == 60 - i + 1L)
            millis += 1.m
        }
        // 1 hour 1 minute spent
    }

    private val Int.m get() = TimeUnit.MINUTES.toMillis(toLong())

    private val Int.h get() = TimeUnit.HOURS.toMillis(toLong())

    private fun test(block: ManagedClock.(EventStatistics) -> Unit) {
        val clock = ManagedClock()
        val stat = EventStatisticsImpl(clock)
        clock.block(stat)
    }
}