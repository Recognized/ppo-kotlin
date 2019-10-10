package fmeasure

import kotlinx.serialization.Serializable
import java.io.*
import java.lang.Double.parseDouble
import java.lang.Long.parseLong
import java.util.*

val br = BufferedReader(InputStreamReader(System.`in`))
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

data class Data(var tp: Int, var fp: Int, var fn: Int)

@Serializable
data class Stat(val f1: Double, val f2: Double, val precision: Double, val recall: Double)

fun fmeasure(CM: Array<Array<Int>>): Stat {
    val K = CM.size

    val count = Array(K) { 0 }
    val stat = Array(K) { Data(0, 0, 0) }
    (0 until K).forEach { c ->
        (0 until K).forEach { t ->
            val v = CM[c][t]
            count[c] += v
            if (c == t) {
                stat[c].tp += v
            } else {
                stat[c].fn += v
                stat[t].fp += v
            }
        }
    }

    val (f1, p, r) = run {
        val precision =
            stat.withIndex().sumByDouble { (index, it) -> if (it.tp == 0) 0.0 else it.tp.toDouble() * count[index] / (it.tp + it.fp) } / count.sum()
        val recall =
            stat.withIndex().sumByDouble { (index, it) -> if (it.tp == 0) 0.0 else it.tp.toDouble() * count[index] / (it.tp + it.fn) } / count.sum()
        val F = if (precision + recall == 0.0) 0.0 else 2.0 * precision * recall / (precision + recall)
        if (F.isNaN()) error("not nan")
        arrayOf(F, precision, recall)
    }

    val f2 = run {
        val F = stat.withIndex().sumByDouble { (index, it) ->
            val precision = if (it.tp == 0) 0.0 else it.tp.toDouble() / (it.tp + it.fp)
            val recall = if (it.tp == 0) 0.0 else it.tp.toDouble() / (it.tp + it.fn)
            val F = if (it.tp == 0) 0.0 else 2.0 * precision * recall / (precision + recall)
            F * count[index]
        } / count.sum()
        if (F.isNaN()) error("not nan")
        F
    }
    return Stat(f1, f2, p, r)
}