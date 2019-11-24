package task4.mvc

import org.jetbrains.exposed.sql.Database
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.jdbc.core.support.JdbcDaoSupport
import javax.sql.DataSource

@SpringBootApplication
open class Application {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(Application::class.java, *args)
        }
    }
}

fun JdbcDaoSupport.database(): org.jetbrains.exposed.sql.Database {
    val `class` = org.jetbrains.exposed.sql.Database.Companion
    val method = `class`::class.java.getDeclaredMethod("connect", DataSource::class.java)
    return method.invoke(jdbcTemplate!!.dataSource!!) as Database
}