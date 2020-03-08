package actors

import akka.actor.*
import kotlinx.coroutines.*
import java.time.Duration
import kotlin.coroutines.CoroutineContext

data class SearchRequest(val query: String)
data class SearchResult(val pages: List<String>)

class SearchSupervisor(private val searchers: Set<Class<SearchConcrete>>) : UntypedAbstractActor() {
    private val results = mutableListOf<SearchResult>()

    override fun supervisorStrategy(): SupervisorStrategy {
        return OneForOneStrategy.stoppingStrategy()
    }

    override fun onReceive(message: Any) {
        when (message) {
            is SearchRequest -> {
                searchers.map {
                    context.actorOf(Props.create(it), it.simpleName)
                }.forEach {
                    it.tell(message, self)
                }
                context.receiveTimeout = Duration.ofSeconds(1)
            }
            is SearchResult -> {
                results += message
                if (results.size == 3)
            }
            is ReceiveTimeout -> {
                context.stop(self)
            }
        }
    }
}

abstract class SearchConcrete : UntypedAbstractActor() {

    abstract suspend fun doSearch(query: String): List<String>

    override fun onReceive(message: Any?) {
        when (message) {
            is SearchRequest -> {
                context.actorOf(Props.create(CoroutineActor::class.java) {
                    CoroutineActor {
                        SearchResult(doSearch(message.query))
                    }
                }).apply {
                    tell(Unit, sender)
                }
            }
        }
    }
}

class CoroutineActor<T>(
    context: CoroutineContext = Dispatchers.IO,
    private val fn: suspend () -> T
) : UntypedAbstractActor(), CoroutineScope {
    private val job = Job()

    override val coroutineContext: CoroutineContext = job + context

    override fun postStop() {
        job.completeExceptionally(CancellationException("Actor stopped"))
    }

    override fun onReceive(message: Any?) {
        val sender = sender()
        launch {
            try {
                sender.tell(fn(), self)
            } catch (ex: CancellationException) {
                // ignore
            } catch (ex: Throwable) {
                sender.tell(ex, self)
            }
        }
    }
}

fun main() {
    ActorSystem.create("MySystem").apply {
        actorOf(Props.create(SearchSupervisor::class.java)).apply {
            tell(SearchRequest("search"), ActorRef.noSender())
        }
    }
}