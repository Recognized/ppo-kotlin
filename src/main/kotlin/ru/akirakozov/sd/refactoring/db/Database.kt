package ru.akirakozov.sd.refactoring.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

interface DbContext {
    fun db(): Database
}

fun <T> DbContext.tx(statement: Transaction.() -> T): T {
    return transaction(Connection.TRANSACTION_SERIALIZABLE, 3, db(), statement)
}