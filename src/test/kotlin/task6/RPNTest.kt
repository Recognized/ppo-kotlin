package task6

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class RPNTest {

    private val okTestCases: List<Pair<String, Int?>> = listOf(
        "1 + 2" to 3,
        "3 * 4" to 12,
        "1 + (15 * 0)" to 1,
        " (1+2) * 3 " to 9,
        "(4 / 2)" to 2,
        "31 / 2" to 15,
        "0 * 0" to 0,
        "5 - 2" to 3,
        "0 - 2 - 3" to -5,
        "(0 - 5) * (0 - 4)" to 20,
        "1" to 1,
        "" to null
    )

    private val notOkTests = listOf(
        "()",
        ")",
        "(",
        "*",
        "-",
        "/",
        "1 + +",
        "1 % 2",
        "0 / 0",
        "1 + ((2 + 3)"
    )

    @Test
    fun `test ok`() {
        okTestCases.forEach { (input, out) ->
            val tokens = Tokenizer(input).collectAll()
            val rpn = ParserVisitor().visit(tokens)
            Assertions.assertEquals(out, CalcVisitor().visit(rpn), PrintVisitor().visit(rpn))
        }
    }

    @Test
    fun `test fail`() {
        notOkTests.forEach { input ->
            Assertions.assertThrows(Throwable::class.java) {
                CalcVisitor().visit(ParserVisitor().visit(Tokenizer(input).collectAll()))
            }
        }
    }
}