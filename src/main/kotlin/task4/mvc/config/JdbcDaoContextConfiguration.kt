package task4.mvc.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DriverManagerDataSource
import task4.mvc.dao.TodoDao
import task4.mvc.dao.TodoJdbcDao

import javax.sql.DataSource

/**
 * @author akirakozov
 */
@Configuration
open class JdbcDaoContextConfiguration {
    @Bean
    open fun todoJdbcDao(dataSource: DataSource): TodoDao {
        return TodoJdbcDao(dataSource)
    }

    @Bean
    open fun dataSource(): DataSource {
        val dataSource = DriverManagerDataSource()
        dataSource.setDriverClassName("org.sqlite.JDBC")
        dataSource.url = "jdbc:sqlite:product.db"
        dataSource.username = ""
        dataSource.password = ""
        return dataSource
    }
}
