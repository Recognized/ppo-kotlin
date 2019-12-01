package task5

interface Drawer {
    fun draw(block: DrawingApi.() -> Unit)

    fun start()
}

interface DrawingApi {
    fun getWidth(): Int
    fun getHeight(): Int
    fun drawCircle(x: Int, y: Int)
    fun drawLabel(label: String, x: Int, y: Int)
    fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int)
}
