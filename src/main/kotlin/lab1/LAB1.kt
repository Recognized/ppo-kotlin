package lab1

import DistanceFn
import DistanceFnClass
import InstanceClass
import KernelFnClass
import Vector
import Window
import WindowS
import br
import close
import distances
import fmeasure.Stat
import fmeasure.fmeasure
import fmeasure.next
import kernels
import knnClass
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list
import manhattan
import me.tongfei.progressbar.ProgressBar
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import sigmoid
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Executors

var FEATURES = 16
var CLASSES = 10

fun normalize(dataset: List<InstanceClass>): List<InstanceClass> {
    val max = (0 until FEATURES).map { i -> dataset.maxBy { it.vector.value[i] }!!.vector.value[i] }
    val min = (0 until FEATURES).map { i -> dataset.minBy { it.vector.value[i] }!!.vector.value[i] }
    return dataset.map {
        InstanceClass(
            Vector(it.vector.value.mapIndexed { i, d -> (d - min[i]) / (max[i] - min[i]) }.toDoubleArray()),
            it.target
        )
    }
}

var uDistanceFnClass: DistanceFnClass = DistanceFnClass(::manhattan.name, manhattan)
var uKernel = KernelFnClass(::sigmoid.name, sigmoid)
var uWindow: Window = Window.Variable(5)

fun test(dataset: List<InstanceClass>, example: Vector): Int {
    val max = (0 until FEATURES).map { i -> dataset.maxBy { it.vector.value[i] }!!.vector.value[i] }
    val min = (0 until FEATURES).map { i -> dataset.minBy { it.vector.value[i] }!!.vector.value[i] }
    val e = Vector(example.value.mapIndexed { i, d -> (d - min[i]) / (max[i] - min[i]) }.toDoubleArray())
    println("Normalized: $e")
    return knnClass(normalize(dataset), uDistanceFnClass.fn, uKernel.fn, e, uWindow)
}

fun interactive() {
    val dataset = readDataset()
    while (true) {
        try {
            when (next().trim()) {
                "distance" -> {
                    val what = next()
                    uDistanceFnClass = distances.first { it.name == what }.let { DistanceFnClass(it.name, it.get()) }
                }
                "run" -> {
                    val vector = Vector(readLine()!!.split(',').map { it.trim().toInt().toDouble() }.toDoubleArray())
                    println("$vector target=${test(dataset, vector)}")
                }
                "kernel" -> {
                    val what = next()
                    uKernel = kernels.first { it.name == what }.let { KernelFnClass(it.name, it.get()) }
                }
                "window" -> {
                    val type = next()
                    val size = next().toDouble()
                    uWindow = when (type) {
                        "fixed" -> Window.Fixed(size)
                        else -> Window.Variable(size.toInt())
                    }
                }
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }
}

fun readDataset(): List<InstanceClass> {
    return timed("Read dataset") {
        CSVParser(br, CSVFormat.DEFAULT).take(4000).map { record ->
            InstanceClass(
                Vector((0..15).map { record[it].trim().toInt().toDouble() }.toDoubleArray()),
                record[16].trim().toInt()
            )
        }
    }
}

fun <T> timed(actionName: String? = null, action: () -> T): T {
    val startTime = System.currentTimeMillis()
    println("[$actionName] Starting...")
    val result = action()
    val endTime = System.currentTimeMillis()
    println("[$actionName]: Done ${(endTime - startTime) / 1000}.${(endTime - startTime) % 1000}s")
    return result
}

fun DistanceFn.cached(dataset: List<InstanceClass>): DistanceFn {
    return timed("cached distance ${this.javaClass.simpleName}") {
        ProgressBar("cached distance ${this.javaClass.simpleName}", dataset.size.toLong()).use { progressBar ->
            val map = mutableMapOf<Pair<Vector, Vector>, Double>()
            for (x in dataset) {
                progressBar.step()
                for (y in dataset) {
                    map[x.vector to y.vector] = this(x.vector, y.vector)
                }
            }
            { x, y ->
                map[x to y]!!
            }
        }
    }
}

fun leaveOneOut(
    dataset: List<InstanceClass>,
    distanceFn: DistanceFnClass,
    kernelFn: KernelFnClass,
    window: Window
): Stat {
    val matrix = Array(CLASSES) { Array(CLASSES) { 0 } }
    timed("LOO ${distanceFn.name} ${kernelFn.name} $window") {
        for (one in dataset) {
            val guess = knnClass(dataset - one, distanceFn.fn, kernelFn.fn, one.vector, window)
            matrix[one.target][guess]++
        }
    }
    val stat = fmeasure(matrix)
    println(stat)
    return stat
}

@Serializable
data class Result(
    val distanceFn: String,
    val kernelFn: String,
    val window: WindowS,
    val stat: Stat
) {
    override fun toString(): String {
        return "[${distanceFn} ${kernelFn} $window]: \t f1=${stat.f1}"
    }
}

fun main() {
    val dataset = normalize(readDataset())
    val threadPool = BlockingExecutor(4, Executors.newFixedThreadPool(4))
    val windows = (3..15 step 2).map { Window.Variable(it) } + (1..9 step 2).map { Window.Fixed(it.toDouble() / 5) }

    val results = ConcurrentLinkedDeque<Result>()
    for (distance in distances.asSequence().map { DistanceFnClass(it.name, it.get().cached(dataset)) }) {
        for (kernel in kernels.map { KernelFnClass(it.name, it.get()) }) {
            for (window in windows) {
                threadPool.execute {
                    val stat = leaveOneOut(dataset, distance, kernel, window)
                    results.add(Result(distance.name, kernel.name, window.toS(), stat))
                }
            }
        }
    }
    for (r in results.sortedByDescending { it.stat.f1 }) {
        println(r)
    }
    File("results_${Date()}.json").bufferedWriter().use {
        it.write(
            Json(JsonConfiguration.Stable.copy(prettyPrint = true)).stringify(
                Result.serializer().list,
                results.toList()
            )
        )
    }

    close()
    while (true) {
        Thread.sleep(20000)
    }
}