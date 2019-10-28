package ru.akirakozov.sd.refactoring.servlet

import kotlinx.html.body
import kotlinx.html.br
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import org.jetbrains.exposed.sql.Database
import ru.akirakozov.sd.refactoring.db.EProduct
import ru.akirakozov.sd.refactoring.db.tx

/**
 * @author akirakozov
 */
class GetProductsServlet(db: Database) : KotlinServlet(db) {

    override fun GetContext.respondHtml(): String {
        return createHTML().html {
            body {
                tx {
                    EProduct.all().forEach {
                        +"${it.name}\t${it.price}"
                        br()
                    }
                }
            }
        }
    }
}
