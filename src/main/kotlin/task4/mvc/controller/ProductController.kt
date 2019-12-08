package task4.mvc.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import task4.mvc.dao.TodoDao
import task4.mvc.view.todoList
import task4.mvc.view.todoLists
import javax.websocket.server.PathParam

@Controller
class ProductController(private val productDao: TodoDao) {

    @PostMapping(value = ["/create-list"])
    fun createTodoList(name: String): String {
        val id = productDao.createTodoList(name)
        return "redirect:/todo/$id"
    }

    @PostMapping(value = ["/todo/{listId}/create-item"])
    fun createTodo(@PathVariable listId: Int, description: String): String {
        productDao.createTodo(listId, description)
        return "redirect:/todo/$listId"
    }

    @PostMapping(value = ["/todo/{listId}/done"])
    fun markAsDone(@PathVariable listId: Int, id: Int): String {
        productDao.markAsDone(id)
        return "redirect:/todo/$listId"
    }

    @PostMapping(value = ["/todo/{listId}/delete"])
    fun delete(@PathVariable listId: Int, id: Int): String {
        productDao.deleteTodo(id)
        return "redirect:/todo/$listId"
    }

    @GetMapping(value = ["/todo/{id}"])
    fun getTodoList(@PathVariable id: Int): ResponseEntity<String> {
        return ResponseEntity(
            todoList(productDao.getTodoLists().first { it.id == id }, productDao.getTodoListContents(id)),
            HttpStatus.OK
        )
    }

    @GetMapping(value = ["/todo"])
    fun getTodoLists(): ResponseEntity<String> {
        return ResponseEntity(
            todoLists(productDao.getTodoLists()),
            HttpStatus.OK
        )
    }
}
