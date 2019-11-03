package crawler

class Logger(val name: String) {

    fun info(msg: () -> Any?) {
        println("[$name]: ${msg()}")
    }
}