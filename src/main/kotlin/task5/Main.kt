package task5

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import kotlin.random.Random

fun main(args: Array<String>) {
    Variants().main(args)
}

class Variants : CliktCommand() {
    val api: Drawer by option("-a").choice("swing" to SwingDrawer(), "fx" to FXDrawer()).default(SwingDrawer())
    val impl: String by option("-i").choice("matrix", "adjacent").default("adjacent")

    override fun run() {
        when (impl) {
            "matrix" -> {
                val size = Random.nextInt(9)
                MatrixGraph(
                    (1..size).map {
                        (1..size).map { Random.nextInt(100) < 15 }.toBooleanArray()
                    }.toTypedArray(),
                    api
                ).redraw()
            }
            "adjacent" -> {
                val size = Random.nextInt(9)
                AdjacencyGraph(
                    (1..size).map {
                        Random.char().toString() to (1..size).map { Random.char().toString() }
                    }.toMap(),
                    api
                ).redraw()
            }
        }
    }
}

fun Random.char() = ('a'.toInt() + Random.nextInt(26)).toChar()