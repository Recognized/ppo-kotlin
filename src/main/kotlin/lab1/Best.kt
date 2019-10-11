package lab1

import DistanceFnClass
import KernelFnClass
import Window
import euclidean
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list
import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.XYSeries
import org.knowm.xchart.style.Styler
import tricube
import java.io.File

fun main() {

    val dataset = normalize(readDataset())

    val best = File("best.json")

    val results = if (best.exists()) {
        best.bufferedReader().use {
            Json(JsonConfiguration.Stable).parse(Result.serializer().list, it.readText())
        }
    } else {
        val results = mutableListOf<Result>()
        val kernel = KernelFnClass(::tricube.name, tricube)
        val distance = DistanceFnClass(::euclidean.name, euclidean.cached(dataset))
        for (window in (1..15).map { Window.Variable(it) } + (1..15).map { Window.Fixed(it / 4.0) }) {
            val stat = leaveOneOut(dataset, distance, kernel, window)
            results.add(Result(distance.name, kernel.name, window.toS(), stat))
        }
        best.bufferedWriter().use {
            it.write(Json(JsonConfiguration.Stable).stringify(Result.serializer().list, results))
        }
        results
    }


    val chart1 = XYChartBuilder().width(2000).height(1000).title("Euclidean tricube variable").xAxisTitle("k").yAxisTitle("F")
        .theme(Styler.ChartTheme.Matlab).build()
    chart1.styler.defaultSeriesRenderStyle = XYSeries.XYSeriesRenderStyle.Line
    chart1.styler.xAxisMax = 15.0
    chart1.styler.xAxisMin = 0.0
    chart1.styler.yAxisMax = 1.0
    chart1.styler.yAxisMin = 0.9

    val chart2 = XYChartBuilder().width(2000).height(1000).title("Euclidean tricube fixed").xAxisTitle("h").yAxisTitle("F")
        .theme(Styler.ChartTheme.Matlab).build()
    chart2.styler.defaultSeriesRenderStyle = XYSeries.XYSeriesRenderStyle.Line
    chart2.styler.xAxisMax = 5.0
    chart2.styler.xAxisMin = 0.0
    chart2.styler.yAxisMax = 1.0
    chart2.styler.yAxisMin = -0.5

    val r1 = results.filter { it.window.k != null }.sortedBy { it.window.k }
    val r2 = results.filter { it.window.h != null }.sortedBy { it.window.h }
    chart1.addSeries("run", r1.map { it.window.k!!.toDouble() }.toDoubleArray(), r1.map { it.stat.f1 }.toDoubleArray())
    chart2.addSeries("run", r2.map { it.window.h!!.toDouble() }.toDoubleArray(), r2.map { it.stat.f1 }.toDoubleArray())
    val wrapper1 = SwingWrapper(chart1)
    val wrapper2 = SwingWrapper(chart2)
    wrapper1.displayChart()
    wrapper2.displayChart()
}