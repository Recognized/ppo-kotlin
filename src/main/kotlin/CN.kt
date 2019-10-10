package cn

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

fun close() = wr.close()

fun main() {
    val N = readInt()
    val M = readInt()
    val K = readInt()

    val classes = Array<MutableList<Int>>(M) { mutableListOf() }

    for (index in 1..N) {
        val v = readInt()
        classes[v - 1].add(index)
    }

    val bank = Array<MutableList<Int>>(K) { mutableListOf() }

    var counter = 0
    classes.forEach {
        it.forEach {
            bank[counter++ % K].add(it)
        }
    }

    bank.forEach {
        write(it.size)
        write(' ')
        it.forEach {
            write(it)
            write(' ')
        }
        writeln()
    }

    close()
}

