package task6

interface Visitor<T> {
    fun accept(token: Token)
    fun get(): T
}

fun <T> Visitor<T>.visit(input: Iterable<Token>): T {
    input.forEach(this::accept)
    return get()
}

class PrintVisitor : Visitor<String> {
    private val buffer = StringBuilder()
    override fun accept(token: Token) {
        if (buffer.isNotEmpty()) {
            buffer.append(" ")
        }
        buffer.append(token.toString())
    }

    override fun get(): String = buffer.toString()
}

class ParserVisitor : Visitor<List<Token>> {
    private val out = mutableListOf<Token>()
    private val stack = mutableListOf<Token>()

    override fun accept(token: Token) {
        when (token) {
            is Number -> {
                out += token
            }
            is Left -> {
                stack += token
            }
            is Right -> {
                if (stack.lastOrNull() is Left) {
                    error("Empty parenthesis")
                }
                while (stack.isNotEmpty()) {
                    val last = stack.lastOrNull() ?: error("Parenthesis mismatch")
                    if (last is Left) {
                        stack.removeAt(stack.lastIndex)
                        return
                    }
                    out += stack.removeAt(stack.lastIndex)
                }
                error("No left parenthesis found")
            }
            is BinOp -> {
                while (stack.isNotEmpty()) {
                    val last = stack.last() as? BinOp ?: break
                    if (last.priority > token.priority || last.leftAssoc && last.priority == token.priority) {
                        out += stack.removeAt(stack.lastIndex)
                    } else {
                        break
                    }
                }
                stack += token
            }
        }
    }

    override fun get(): List<Token> {
        assert(stack.none { it is Left || it is Right }) { "Parenthesis mismatch" }
        return out + stack.reversed()
    }
}

class CalcVisitor : Visitor<Int?> {
    private val stack = mutableListOf<Int>()

    override fun accept(token: Token) {
        when (token) {
            is Number -> stack += token.value
            else -> {
                val operation: (Int, Int) -> Int = when (token) {
                    is Div -> Int::div
                    is Mul -> Int::times
                    is Minus -> Int::minus
                    is Plus -> Int::plus
                    else -> error("Unreachable code")
                }
                val b = stack.lastOrNull() ?: error("Mismatched input")
                stack.removeAt(stack.lastIndex)
                val a = stack.lastOrNull() ?: error("Mismatched input")
                stack.removeAt(stack.lastIndex)
                stack += operation(a, b)
            }
        }
    }

    override fun get(): Int? = if (stack.size > 1) error("Mismatched brackets in input") else stack.firstOrNull()
}
