package lab2

import task2.AppAuth

object MockAuth : AppAuth {
    override val id: Int = 0
    override val secretKey: String = "secret"
    override val serviceKey: String = "service"
    override val version: String = "1.0"
}