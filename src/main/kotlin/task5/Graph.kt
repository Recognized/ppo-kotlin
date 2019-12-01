package task5

import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

abstract class Graph(val drawer: Drawer) {

    abstract val vertices: Collection<String>

    abstract fun forEach(consumer: (String, String) -> Unit)

    fun redraw() {
        drawer.draw {
            if (vertices.isEmpty()) return@draw

            val (centerX, centerY) = getWidth() / 2 to getHeight() / 2
            val diameter = 0.5 * min(centerX, centerY)

            val deltaAngle = 2 * Math.PI / vertices.count()

            var angle = 0.0

            val positions = mutableMapOf<String, Pair<Int, Int>>()

            for (vertex in vertices) {
                val x = (diameter * cos(angle)).toInt() + centerX
                val y = (diameter * sin(angle)).toInt() + centerY
                positions[vertex] = x to y
                angle += deltaAngle
            }

            forEach { a, b ->
                val (x1, y1) = positions[a]!!
                val (x2, y2) = positions[b]!!
                drawLine(x1, y1, x2, y2)
            }

            for (vertex in vertices) {
                val (x, y) = positions[vertex]!!
                drawCircle(x, y)
                drawLabel(vertex, x, y)
            }
        }
        drawer.start()
    }
}