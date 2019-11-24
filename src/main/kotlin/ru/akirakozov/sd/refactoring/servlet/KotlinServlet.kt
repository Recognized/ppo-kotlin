package ru.akirakozov.sd.refactoring.servlet

import org.jetbrains.exposed.sql.Database
import ru.akirakozov.sd.refactoring.db.DbContext
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponse.SC_OK

abstract class KotlinServlet(private val database: Database) : HttpServlet(), DbContext {
    override fun db(): Database = database

    abstract fun GetContext.respondHtml(): String

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        val html = GetContext(resp, req).respondHtml()
        resp.writer.println(html)
        resp.status = SC_OK
        resp.contentType = "text/html"
    }
}

class GetContext(val response: HttpServletResponse, val request: HttpServletRequest)

fun GetContext.get(parameter: String): String? = request.getParameter(parameter)