package task6

class Tokenizer(private val input: CharSequence) {
    private var position = -1

    fun next(): Token? {
        consumeWhitespace()
        return when {
            position + 1 < input.length -> {
                producers.asSequence().mapNotNull { p ->
                    val match = p.regex.find(input, startIndex = position + 1)
                    match?.takeIf { it.range.first == position + 1 }?.let {
                        position += it.value.length
                        p.fn(it.value)
                    }
                }.firstOrNull() ?: error("Could not parse input at position=${position + 1}")
            }
            else -> null
        }
    }

    fun collectAll(): List<Token> {
        val out = mutableListOf<Token>()
        var next = next()
        while (next != null) {
            out += next
            next = next()
        }
        return out
    }

    private fun consumeWhitespace() {
        while (input.getOrNull(position + 1)?.isWhitespace() == true) {
            position++
        }
    }

    companion object {
        private val producers: List<Producer> = listOf(
            Producer("\\(".toRegex()) { Left },
            Producer("\\)".toRegex()) { Right },
            Producer("\\+".toRegex()) { Plus },
            Producer("-".toRegex()) { Minus },
            Producer("\\*".toRegex()) { Mul },
            Producer("/".toRegex()) { Div },
            Producer("[0123456789]+".toRegex()) { Number(it.toInt()) }
        )
    }

    private class Producer(val regex: Regex, val fn: (String) -> Token)
}

sealed class Token

abstract class BinOp(val priority: Int, val leftAssoc: Boolean) : Token()

object Left : Token() {
    override fun toString(): String = "Left"
}

object Right : Token() {
    override fun toString(): String = "Right"
}

object Plus : BinOp(1, false) {
    override fun toString(): String = "Plus"
}

object Minus : BinOp(2, true) {
    override fun toString(): String = "Minus"
}

object Mul : BinOp(2, false) {
    override fun toString(): String = "Mul"
}

object Div : BinOp(2, true) {
    override fun toString(): String = "Div"
}

data class Number(val value: Int) : Token() {
    override fun toString(): String = "Number($value)"
}
