package ru.akirakozov.sd.refactoring.db

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object Schema {

    object Product : IntIdTable("PRODUCT") {
        val name = text("NAME")
        val price = long("PRICE")
    }
}

class EProduct(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<EProduct>(Schema.Product)

    var name by Schema.Product.name
    var price by Schema.Product.price
}