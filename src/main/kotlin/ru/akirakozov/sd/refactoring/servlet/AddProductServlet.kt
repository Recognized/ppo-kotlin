package ru.akirakozov.sd.refactoring.servlet

import org.jetbrains.exposed.sql.Database
import ru.akirakozov.sd.refactoring.db.EProduct
import ru.akirakozov.sd.refactoring.db.tx

/**
 * @author akirakozov
 */
class AddProductServlet(db: Database) : KotlinServlet(db) {

    override fun GetContext.respondHtml(): String {
        val name = get("name")!!
        val price = get("price")!!.toLong()

        tx {
            EProduct.new {
                this.name = name
                this.price = price
            }
        }

        return "OK"
    }
}
