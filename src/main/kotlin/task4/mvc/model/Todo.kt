package task4.mvc.model

import task4.mvc.dao.TID

data class Todo(val id: TID, val description: String, val done: Boolean)

data class TodoList(val id: TID, val name: String)