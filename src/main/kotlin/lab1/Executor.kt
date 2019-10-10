package lab1

import com.sun.corba.se.impl.orbutil.concurrent.SyncUtil.acquire
import java.util.concurrent.Semaphore
import java.util.concurrent.Executor


class BlockingExecutor(concurrentTasksLimit: Int, val delegate: Executor) :
    Executor {
    private val semaphore: Semaphore = Semaphore(concurrentTasksLimit)

    override fun execute(command: Runnable) {
        try {
            semaphore.acquire()
        } catch (e: InterruptedException) {
            e.printStackTrace()
            return
        }

        val wrapped = {
            try {
                command.run()
            } finally {
                semaphore.release()
            }
        }

        delegate.execute(wrapped)
    }
}