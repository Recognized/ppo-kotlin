package lab3

import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.akirakozov.sd.refactoring.Main
import ru.akirakozov.sd.refactoring.db.EProduct
import ru.akirakozov.sd.refactoring.db.Schema
import java.io.File
import java.sql.Connection

class TestShop {
    private val client = HttpClient()

    @Test
    fun `can add products`() {
        withServer {
            addProduct("iphone", 300)
            addProduct("cheese", 500)
            getProducts() respond "simple"
        }
    }

    @Test
    fun `allow duplicates`() {
        withServer {
            addProduct("p1", 100)
            addProduct("p1", 100)
            getProducts() respond "getDuplicates"
        }
    }

    @Test
    fun `test query commands`() {
        withServer {
            addProduct("p1", 10)
            addProduct("p2", 20)
            addProduct("p3", 30)
            query("min") respond "minQuery"
            query("max") respond "maxQuery"
            query("count") respond "countQuery"
            query("sum") respond "sumQuery"
        }
    }

    @Test
    fun `don't allow other requests`() {
        assertThrows<ClientRequestException> {
            withServer {
                client.get<String>("http://localhost:8081/404")
            }
        }
    }

    @Test
    fun `get empty`() {
        withServer {
            getProducts() respond "getEmpty"
            query("min") respond "minEmpty"
            query("max") respond "maxEmpty"
            query("sum") respond "sumEmpty"
            query("count") respond "countEmpty"
        }
    }

    private infix fun String.respond(filename: String) {
        val file = File("src/test/resources/$filename.html")
        if (file.exists()) {
            Assertions.assertEquals(file.readText().trim(), trim())
        } else {
            file.createNewFile()
            file.bufferedWriter().use {
                it.write(this.trim())
            }
        }
    }

    private suspend fun addProduct(name: String, price: Int) {
        client.get<String>("http://localhost:8081/add-product") {
            parameter("name", name)
            parameter("price", price)
        }
    }

    private suspend fun getProducts(): String {
        return client.get("http://localhost:8081/get-products")
    }

    private suspend fun query(command: String): String {
        return client.get("http://localhost:8081/query") {
            parameter("command", command)
        }
    }

    private fun withServer(action: suspend () -> Unit) {
        val server = Main.runServer(emptyArray())
        val db = Database.connect("jdbc:sqlite:test.db", driver = "org.sqlite.JDBC")
        transaction(Connection.TRANSACTION_SERIALIZABLE, 3, db) {
            Schema.Product.deleteAll()
        }
        try {
            runBlocking {
                action()
            }
        } finally {
            server.stop()
        }
    }
}