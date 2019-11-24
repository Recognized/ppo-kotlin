package task4.mvc.view

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import task4.mvc.model.Todo
import task4.mvc.model.TodoList

fun todoList(list: TodoList, content: List<Todo>): String {
    return createHTML().html {
        head {
            meta(charset = "utf-8")
            title("Todo App")
        }
        body {
            h3 {
                +list.name
            }
            div {
                ul {
                    for (item in content) {
                        li {
                            if (item.done) {
                                del {
                                    +item.description
                                }
                            } else {
                                +item.description
                            }
                            form("/todo/${list.id}/delete", method = FormMethod.post) {
                                hiddenInput(name = "id") {
                                    value = item.id.toString()
                                }
                                button(type = ButtonType.submit) {
                                    +"Delete todo"
                                }
                            }
                            if (!item.done) {
                                form("/todo/${list.id}/done", method = FormMethod.post) {
                                    hiddenInput(name = "id") {
                                        value = item.id.toString()
                                    }
                                    button(type = ButtonType.submit) {
                                        +"Mark as done"
                                    }
                                }
                            }
                        }
                    }
                }

                br()

                div {
                    form(action = "/todo/${list.id}/create-item", method = FormMethod.post) {
                        textInput(name = "description")
                        button {
                            +"Create todo item"
                        }
                    }
                }
            }
        }
    }
}