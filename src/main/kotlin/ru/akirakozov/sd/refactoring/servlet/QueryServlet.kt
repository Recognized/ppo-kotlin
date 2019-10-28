package ru.akirakozov.sd.refactoring.servlet

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.exposed.sql.*
import ru.akirakozov.sd.refactoring.db.DbContext
import ru.akirakozov.sd.refactoring.db.EProduct
import ru.akirakozov.sd.refactoring.db.Schema
import ru.akirakozov.sd.refactoring.db.Schema.Product
import ru.akirakozov.sd.refactoring.db.tx
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.io.IOException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement

/**
 * @author akirakozov
 */
class QueryServlet(db: Database) : KotlinServlet(db) {

    override fun GetContext.respondHtml(): String {
        return createHTML().html {
            body {
                when (val command = get("command")) {
                    "max" -> {
                        showMaxMin(command, maxPrice())
                    }
                    "min" -> {
                        showMaxMin(command, minPrice())
                    }
                    "sum" -> {
                        +"Summary price: ${sumPrice() ?: 0}"
                    }
                    "count" -> {
                        +"Number of products: ${count()}"
                    }
                    else -> {
                        +"Unknown command: $command"
                    }
                }
            }
        }
    }

    private fun BODY.showMaxMin(word: String, product: EProduct?) {
        h1 {
            +"Product with $word price:\n"
        }
        product?.let {
            +"${it.name} ${it.price}"
            br()
        }
    }

    private fun maxMinPrice(sortOrder: SortOrder): EProduct? = tx {
        Product.selectAll().orderBy(Product.price, sortOrder).limit(1).singleOrNull()?.let { EProduct.wrapRow(it) }
    }

    private fun maxPrice() = maxMinPrice(SortOrder.DESC)
    private fun minPrice() = maxMinPrice(SortOrder.ASC)

    private fun sumPrice(): Long? = tx {
        val sum = Product.price.sum().alias("sum")
        return@tx Product.slice(sum).selectAll().singleOrNull()?.let { it[sum] }
    }

    private fun count(): Int = tx {
        EProduct.count()
    }
}
