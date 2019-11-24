package task4.mvc.view

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import task4.mvc.model.TodoList

fun todoLists(data: List<TodoList>): String {
    return createHTML().html {
        head {
            meta(charset = "utf-8")
            title("Todo App")
        }
        body {
            div {
                ul {
                    for (list in data) {
                        li {
                            a(href = "/todo/${list.id}") {
                                +list.name
                            }
                        }
                    }
                }

                br()

                div {
                    form(action = "/create-list", method = FormMethod.post) {
                        textInput(name = "name")
                        button {
                            +"Create todo list"
                        }
                    }
                }
            }
        }
    }
}