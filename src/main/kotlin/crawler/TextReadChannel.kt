package crawler

import kotlinx.coroutines.io.ByteReadChannel
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

interface TextReadChannel {
    suspend fun next(): Int
}

class TextReadChannelImpl(
    private val limit: Long,
    private val channel: ByteReadChannel,
    charset: Charset = Charsets.UTF_8
) :
    TextReadChannel {
    private var count = 0L
    private var offset = 0
    private var charBuffer = CharBuffer.allocate(8192 + 100)
    private val array = ByteBuffer.allocate(8192)
    private val decoder = charset.newDecoder()

    init {
        decoder.onMalformedInput(CodingErrorAction.REPORT)
        decoder.onUnmappableCharacter(CodingErrorAction.REPLACE)
    }

    override suspend fun next(): Int {
        if (count >= limit) {
            return -1
        }
        if (!charBuffer.hasRemaining()) {
            if (!loadMore()) {
                count = limit
                return -1
            }
        }
        count++
        return charBuffer.get().toInt()
    }

    private suspend fun loadMore(): Boolean {
        charBuffer.clear()
        while (charBuffer.position() == 0) {
            val count = channel.readAvailable(array)
            if (count == 0) {
                return false
            }
            array.flip()
            val error = decoder.decode(array, charBuffer, false)
            assert(!error.isOverflow)
            if (error.isMalformed) {
                println(error)
            }
            if (array.hasRemaining()) {
                val excessive = ByteArray(array.remaining())
                array.get(excessive)
                array.clear()
                array.put(excessive)
            } else {
                array.clear()
            }
        }
        charBuffer.flip()
        offset = 0
        return true
    }
}
