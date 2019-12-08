package task7

import kotlin.math.max

interface Clock {
    fun currentTimeMillis(): Long
}

class SystemClock : Clock {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}

class ManagedClock : Clock {
    var millis: Long = 0
        set(value) {
            field = max(field, value)
        }

    override fun currentTimeMillis(): Long = millis
}