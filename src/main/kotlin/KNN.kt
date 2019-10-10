import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.*
import java.lang.Double.parseDouble
import java.lang.Long.parseLong
import java.util.*
import kotlin.math.*

val br = File("dataset.csv").bufferedReader()
var st: StringTokenizer? = null

fun next(): String {
    while (st == null || !st!!.hasMoreElements()) {
        try {
            st = StringTokenizer(br.readLine())
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
    return st!!.nextToken()
}

fun readInt(): Int = Integer.parseInt(next())
fun readLong(): Long = parseLong(next())
fun readDouble() = parseDouble(next())
fun readLine(): String? = br.readLine()

val wr = BufferedWriter(PrintWriter(System.out))
fun write(int: Int) = wr.write(int.toString())
fun write(char: Char) = wr.write(char.toString())
fun write(string: String) = wr.write(string)
fun writeln() = wr.newLine()
fun write(double: Double) = wr.write(double.toString())

fun close() = wr.close()

class Vector(val value: DoubleArray) {
    override fun toString(): String = value.joinToString(prefix = "[", postfix = "]")
}

data class Instance(val vector: Vector, val target: Double)
data class InstanceClass(val vector: Vector, val target: Int)

fun Double.squared() = this * this

class DistanceFnClass(val name: String, @Transient val fn: (Vector, Vector) -> Double) {
    operator fun invoke(v: Vector, x: Vector) = fn(v, x)
}

class KernelFnClass(val name: String, val fn: (Double) -> Double) {
    operator fun invoke(v: Double) = fn(v)
}

typealias DistanceFn = (Vector, Vector) -> Double
typealias KernelFn = (Double) -> Double

sealed class Window {
    data class Fixed(val h: Double) : Window() {
        override fun toString(): String = "fixed $h"
        override fun toS(): WindowS = WindowS(true, null, h)
    }

    data class Variable(val k: Int) : Window() {
        override fun toString(): String = "variable $k"
        override fun toS(): WindowS = WindowS(false, k, null)
    }

    abstract fun toS(): WindowS
}

@Serializable
data class WindowS(val fixed: Boolean, val k: Int?, val h: Double?)

val manhattan: DistanceFn = { x, y ->
    x.value.zip(y.value).sumByDouble { abs(it.first - it.second) }
}

val euclidean: DistanceFn = { x, y ->
    sqrt(x.value.zip(y.value).sumByDouble { (it.first - it.second).squared() })
}

val chebyshev: DistanceFn = { x, y ->
    x.value.zip(y.value).map { abs(it.first - it.second) }.max()!!
}

val uniform: KernelFn = {
    if (it > -0.99999 && it < 0.99999) 0.5 else 0.0
}

val triangular: KernelFn = {
    if (it >= -1.0 && it <= 1.0) 1.0 - abs(it) else 0.0
}

val epanechnikov: KernelFn = {
    if (it >= -1.0 && it <= 1.0) 0.75 * (1.0 - it * it) else 0.0
}

val quartic: KernelFn = {
    if (it >= -1.0 && it <= 1.0) 15.0 * (1.0 - it * it).squared() / 16.0 else 0.0
}

val triweight: KernelFn = {
    if (it >= -1.0 && it <= 1.0) 35.0 * (1.0 - it * it).pow(3.0) / 32.0 else 0.0
}

val tricube: KernelFn = {
    if (it >= -1.0 && it <= 1.0) 70.0 * (1.0 - abs(it * it * it)).pow(3.0) / 81.0 else 0.0
}

val gaussian: KernelFn = {
    1.0 / sqrt(2.0 * Math.PI) * exp(-0.5 * it * it)
}

val cosine: KernelFn = {
    if (it >= -1.0 && it <= 1.0) Math.PI / 4.0 * cos(0.5 * Math.PI * it) else 0.0
}

val logistic: KernelFn = {
    1.0 / (exp(it) + 2 + exp(-it))
}

val sigmoid: KernelFn = {
    2 / Math.PI / (exp(it) + exp(-it))
}


fun knn(
    dataset: List<Instance>,
    distanceFn: DistanceFn,
    kernel: KernelFn,
    subject: Vector,
    window: Window
): Double {
    val kernelFn: (Double, Double) -> Double = { x, y ->
        if (x.nearZero() && y.nearZero()) kernel(0.0) else kernel(x / y)
    }
    return when (window) {
        is Window.Fixed -> {
            val t = dataset.sumByDouble { it.target * kernelFn(distanceFn(it.vector, subject), window.h) }
            val b = dataset.sumByDouble { kernelFn(distanceFn(it.vector, subject), window.h) }
            if (t.nearZero() && b.nearZero()) {
                return dataset
                    .filter { it.vector.value.close(subject.value) }
                    .takeIf { it.isNotEmpty() }
                    ?.map { it.target }
                    ?.average() ?: dataset.map { it.target }.average()
            }
            t / b
        }
        is Window.Variable -> {
            val subset = dataset.sortedWith(
                compareBy<Instance> {
                    distanceFn(
                        it.vector,
                        subject
                    )
                }.thenByDescending { it.target }
            ).take(window.k + 1)
            val h = distanceFn(subset[window.k].vector, subject)
            val dd = subset.map { distanceFn(it.vector, subject) }
            if (dd.all { x -> x.isSame(dd.first()) }) {
                return if (h.nearZero()) {
                    dataset.filter { it.vector.value.close(subject.value) }.map { it.target }.average()
                } else {
                    dataset.map { it.target }.average()
                }
            }
            val t = subset.sumByDouble { it.target * kernelFn(distanceFn(it.vector, subject), h) }
            val b = subset.sumByDouble { kernelFn(distanceFn(it.vector, subject), h) }
            t / b
        }
    }
}

fun knnClass(
    dataset: List<InstanceClass>,
    distanceFn: DistanceFn,
    kernel: KernelFn,
    subject: Vector,
    window: Window
): Int {
    val kernelFn: (Double, Double) -> Double = { x, y ->
        if (x.nearZero() && y.nearZero()) kernel(0.0) else kernel(x / y)
    }
    return when (window) {
        is Window.Fixed -> {
            val t = dataset.groupBy { it.target }
                .mapValues { it.value.sumByDouble { kernelFn(distanceFn(it.vector, subject), window.h) } }
            t.maxBy { it.value }!!.key
        }
        is Window.Variable -> {
            val subset = dataset.sortedWith(
                compareBy<InstanceClass> {
                    distanceFn(
                        it.vector,
                        subject
                    )
                }.thenByDescending { it.target }
            ).take(window.k + 1)
            val h = distanceFn(subset[window.k].vector, subject)
            val t = dataset.groupBy { it.target }
                .mapValues { it.value.sumByDouble { kernelFn(distanceFn(it.vector, subject), h) } }
            t.maxBy { it.value }!!.key
        }
    }
}

fun Double.isSame(other: Double) = isInfinite() && other.isInfinite() || abs(this - other).nearZero()

val distances = listOf(::manhattan, ::euclidean, ::chebyshev)
val kernels = listOf(
    ::uniform,
    ::epanechnikov,
    ::cosine,
    ::sigmoid,
    ::cosine,
    ::tricube,
    ::gaussian,
    ::logistic,
    ::triweight,
    ::quartic,
    ::triangular
)

fun DoubleArray.close(other: DoubleArray) = chebyshev(Vector(this), Vector(other)) < 1e-10
fun Double.nearZero() = abs(this) < 1e-10

fun main() {
    val n = readInt()
    val m = readInt()
    val dataset = (1..n).map {
        Instance(Vector((1..m).map { readInt().toDouble() }.toDoubleArray()), readInt().toDouble())
    }
    val instance = Vector((1..m).map { readInt().toDouble() }.toDoubleArray())
    val distance = readLine()?.let { name -> distances.find { it.name == name }?.get() }!!
    val kernel = readLine()?.let { name -> kernels.find { it.name == name }?.get() }!!
    val window = when (readLine()!!) {
        "fixed" -> Window.Fixed(readInt().toDouble())
        else -> Window.Variable(readInt())
    }
    write(knn(dataset, distance, kernel, instance, window).also { if (it.isNaN()) error("error") })
    close()
}