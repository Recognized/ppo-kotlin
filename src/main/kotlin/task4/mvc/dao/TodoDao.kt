package task4.mvc.dao

import task4.mvc.model.Todo
import task4.mvc.model.TodoList

typealias TID = Int

interface TodoDao {

    fun getTodoLists(): List<TodoList>
    fun getTodoListContents(id: TID): List<Todo>
    fun createTodoList(name: String): TID
    fun createTodo(listId: TID, description: String): TID
    fun markAsDone(id: TID)
    fun deleteTodo(id: TID)
}
