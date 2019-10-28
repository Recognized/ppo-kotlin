package ru.akirakozov.sd.refactoring

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import ru.akirakozov.sd.refactoring.db.Schema
import ru.akirakozov.sd.refactoring.servlet.AddProductServlet
import ru.akirakozov.sd.refactoring.servlet.GetProductsServlet
import ru.akirakozov.sd.refactoring.servlet.QueryServlet
import java.sql.Connection

fun main() {
    Main.runServer(emptyArray()).join()
}

/**
 * @author akirakozov
 */
object Main {

    @Throws(Exception::class)
    @JvmStatic
    fun runServer(args: Array<String>): Server {
        val db = Database.connect("jdbc:sqlite:test.db", driver = "org.sqlite.JDBC")
        transaction(Connection.TRANSACTION_SERIALIZABLE, 3, db) {
            SchemaUtils.create(Schema.Product)
        }


        val server = Server(8081)

        val context = ServletContextHandler(ServletContextHandler.SESSIONS)
        context.contextPath = "/"
        server.handler = context

        context.addServlet(ServletHolder(AddProductServlet(db)), "/add-product")
        context.addServlet(ServletHolder(GetProductsServlet(db)), "/get-products")
        context.addServlet(ServletHolder(QueryServlet(db)), "/query")

        server.start()
        return server
    }
}
