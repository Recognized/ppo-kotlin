package task5

class MatrixGraph(val matrix: Array<BooleanArray>, drawer: Drawer) : Graph(drawer) {

    init {
        assert(matrix.all { it.size == matrix.size })
    }

    override val vertices: Collection<String> = (0 until matrix.size).map { it.toString() }

    override fun forEach(consumer: (String, String) -> Unit) {
        for (i in matrix.indices) {
            for (j in matrix.indices) {
                if (matrix[i][j]) {
                    consumer(i.toString(), j.toString())
                }
            }
        }
    }
}

class AdjacencyGraph(val adjacent: Map<String, List<String>>, drawer: Drawer) : Graph(drawer) {
    override val vertices: Collection<String> = adjacent.keys + adjacent.values.flatten().toSet()

    override fun forEach(consumer: (String, String) -> Unit) {
        adjacent.forEach { (from, to) ->
            to.forEach {
                consumer(from, it)
            }
        }
    }
}