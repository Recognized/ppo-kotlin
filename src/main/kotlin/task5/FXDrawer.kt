package task5

import javafx.embed.swing.JFXPanel
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.text.Font
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.SwingUtilities

class FXDrawer : Drawer {
    private val width = 1000
    private val height = 1000
    private var draw: (DrawingApi.() -> Unit)? = null
    private val panel by lazy {
        val frame = JFrame()
        frame.size = Dimension(width, height)
        val panel = JFXPanel()
        frame.add(panel)
        val canvas = Canvas(width.toDouble(), height.toDouble())
        val gc = canvas.graphicsContext2D!!
        val root = Group()
        draw?.invoke(FXDrawingApi(gc))
        root.children.add(canvas)
        panel.scene = Scene(root, Color.WHITE)
        frame
    }

    override fun start() {
        SwingUtilities.invokeLater {
            panel.isVisible = true
        }
    }

    override fun draw(block: DrawingApi.() -> Unit) {
        draw = block
    }

    private inner class FXDrawingApi(private val gc: GraphicsContext) : DrawingApi {
        override fun getWidth(): Int = width
        override fun getHeight(): Int = height

        override fun drawCircle(x: Int, y: Int) {
            gc.fill = Color.BLACK
            gc.fillOval(x.toDouble() - 45, y.toDouble() - 45, 90.0, 90.0)
            gc.fill = Color.WHITE
            gc.fillOval(x.toDouble() - 44, y.toDouble() - 44, 88.0, 88.0)
            gc.fill = Color.BLACK
        }

        override fun drawLabel(label: String, x: Int, y: Int) {
            gc.fill = Color.BLACK
            gc.font = Font.font(32.0)
            gc.strokeText(label, x.toDouble() - 12, y.toDouble() + 12)
        }

        override fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int) {
            gc.strokeLine(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())
        }
    }
}