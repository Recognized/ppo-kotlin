package task2

interface AppAuth {
    val id: Int
    val secretKey: String
    val serviceKey: String
    val version: String
}

object SecretAuth : AppAuth {
    override val id = 7178573
    override val secretKey = System.getProperty("secretKey") ?: error("Pass secret key")
    override val serviceKey = System.getProperty("serviceKey") ?: error("Pass service key")
    override val version = "5.102"
}



