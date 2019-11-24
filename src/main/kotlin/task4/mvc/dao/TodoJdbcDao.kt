package task4.mvc.dao

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.jdbc.core.support.JdbcDaoSupport
import ru.akirakozov.sd.refactoring.db.DbContext
import ru.akirakozov.sd.refactoring.db.tx
import task4.mvc.model.Todo
import task4.mvc.model.TodoList
import javax.sql.DataSource

/**
 * @author akirakozov
 */
class TodoJdbcDao(val dataSource: DataSource) : TodoDao, DbContext {
    val db = Database.connect(dataSource)

    init {
        tx {
            SchemaUtils.createMissingTablesAndColumns(Schema.Todo, Schema.TodoList)
        }
    }

    override fun db(): Database = db

    override fun getTodoLists(): List<TodoList> {
        return tx {
            ETodoList.all().map { TodoList(it.id.value, it.name) }
        }
    }

    override fun getTodoListContents(id: TID): List<Todo> {
        return tx {
            ETodo.all().filter { it.listId.id.value == id }.map {
                Todo(it.id.value, it.description, it.done)
            }
        }
    }

    override fun createTodoList(name: String): TID {
        return tx {
            ETodoList.new {
                this.name = name
            }.id.value
        }
    }

    override fun createTodo(listId: TID, description: String): TID {
        return tx {
            Schema.Todo.insertAndGetId {
                it[this.listId] = EntityID(listId, Schema.TodoList)
                it[this.description] = description
            }.value
        }
    }

    override fun markAsDone(id: TID) {
        tx {
            ETodo[id].done = true
        }
    }

    override fun deleteTodo(id: TID) {
        tx {
            ETodo[id].delete()
        }
    }
}


object Schema {

    object TodoList : IntIdTable("TodoList") {
        val name = varchar("name", 200)
    }

    object Todo : IntIdTable("Todo") {
        val listId = reference("list_id", TodoList.id)
        val description = varchar("description", 300)
        val done = bool("done").default(false)
    }
}

class ETodoList(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ETodoList>(Schema.TodoList)

    var name by Schema.TodoList.name
}

class ETodo(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ETodo>(Schema.Todo)

    var listId by ETodoList referencedOn Schema.Todo.listId
    var description by Schema.Todo.description
    var done by Schema.Todo.done
}