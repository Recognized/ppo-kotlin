package task7

import java.util.*
import java.util.concurrent.TimeUnit

interface EventStatistics {
    fun incEvent(name: String)
    fun getEventStatisticsByName(name: String): Statistics
    fun getAllEventStatistics(): List<Statistics>
    fun printStatistics()
}

data class Statistics(val name: String, val times: Long)

class EventStatisticsImpl(val clock: Clock, val period: Long = TimeUnit.HOURS.toMillis(1)) : EventStatistics {
    private val stat = mutableMapOf<String, EventStat>()

    private fun getOrCompute(name: String) = stat.computeIfAbsent(name) { EventStat(it, clock, period) }

    override fun incEvent(name: String) {
        getOrCompute(name).record()
    }

    override fun getEventStatisticsByName(name: String): Statistics {
        return stat[name]?.stat() ?: Statistics(name, 0)
    }

    override fun getAllEventStatistics(): List<Statistics> {
        return stat.values.map { it.stat() }
    }

    override fun printStatistics() {
        getAllEventStatistics().forEach {
            println("\"${it.name}\" -- ${it.times.toDouble() / TimeUnit.HOURS.toMinutes(1)} rpm")
        }
    }
}

class EventStat(val name: String, val clock: Clock, val period: Long) {
    private val occurrences = ArrayDeque<Long>()

    fun record() {
        occurrences.add(clock.currentTimeMillis())
        clearOld()
    }

    fun stat(): Statistics {
        clearOld()
        return Statistics(
            name,
            occurrences.size.toLong()
        )
    }

    private fun clearOld() {
        val iter = occurrences.iterator()
        val now = clock.currentTimeMillis()
        while (iter.hasNext()) {
            val next = iter.next()
            if (now - next > period) {
                iter.remove()
            }
        }
    }
}