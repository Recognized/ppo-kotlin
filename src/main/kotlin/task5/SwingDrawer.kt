package task5

import java.awt.*
import java.awt.geom.Ellipse2D
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities

class SwingDrawer : Drawer {
    private var block: (DrawingApi.() -> Unit)? = null

    override fun start() {
        object : JPanel(BorderLayout()) {
            override fun paintComponent(g: Graphics) {
                block?.invoke(Graphics2DDrawingApi(g as Graphics2D))
            }
        }.also {
            SwingUtilities.invokeLater {
                val window = JFrame()
                window.setSize(1400, 900)
                window.add(it)
                window.isVisible = true
                it.isVisible = true
            }
        }
    }

    override fun draw(block: DrawingApi.() -> Unit) {
        this.block = block
    }

    private inner class Graphics2DDrawingApi(val graphics: Graphics2D) : DrawingApi {
        init {
            graphics.color = Color.WHITE
            graphics.fillRect(0, 0, getWidth(), getHeight())
            graphics.color = Color.BLACK
            graphics.font = Font("Consolas", 48, 48)
            graphics.stroke = BasicStroke(2.0f)
        }

        override fun getWidth(): Int = graphics.clipBounds.width

        override fun getHeight(): Int = graphics.clipBounds.height

        override fun drawCircle(x: Int, y: Int) {
            val circle = Ellipse2D.Double(x.toDouble() - 45, y.toDouble() - 45, 90.0, 90.0)
            graphics.color = Color.WHITE
            graphics.fill(circle)
            graphics.color = Color.BLACK
            graphics.draw(circle)
        }

        override fun drawLabel(label: String, x: Int, y: Int) {
            graphics.drawString(label, x - 12, y + 12)
        }

        override fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int) {
            graphics.drawLine(x1, y1, x2, y2)
        }
    }
}