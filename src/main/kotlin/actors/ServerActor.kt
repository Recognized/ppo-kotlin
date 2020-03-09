package actors

import akka.actor.*
import kotlinx.coroutines.*
import java.time.Duration
import kotlin.coroutines.CoroutineContext

data class SearchRequest(val query: String)
data class SearchResult(val provider: String, val pages: List<String>)
data class TotalResult(val results: List<SearchResult>)

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
                if (results.size == 3) {
                    context.parent.tell(TotalResult(results.toList()), self)
                    context.stop(self)
                }
            }
            is ReceiveTimeout -> {
                context.parent.tell(TotalResult(results.toList()), self)
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
                        SearchResult(this::class.java.simpleName, doSearch(message.query))
                    }
                }).apply {
                    tell(Unit, sender)
                }
            }
        }
    }
}

private fun fakeSearch(): List<String> {
    return (1..10).map {
        "$it"
    }
}

open class GoogleSearch : SearchConcrete() {
    override suspend fun doSearch(query: String): List<String> = fakeSearch()
}

open class YandexSearch : SearchConcrete() {
    override suspend fun doSearch(query: String): List<String> = fakeSearch()
}

open class BingSearch : SearchConcrete() {
    override suspend fun doSearch(query: String): List<String> = fakeSearch()
}

class SlowYandexSearch : YandexSearch() {
    override suspend fun doSearch(query: String): List<String> {
        delay(5000)
        return super.doSearch(query)
    }
}

class SlowBingSearch : BingSearch() {
    override suspend fun doSearch(query: String): List<String> {
        delay(5000)
        return super.doSearch(query)
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

class SearchPrinter(private val searchers: Set<Class<SearchConcrete>>) : UntypedAbstractActor() {
    override fun onReceive(message: Any) {
        when (message) {
            is TotalResult -> {
                println(message)
            }
            else -> {
                context.actorOf(Props.create(SearchSupervisor::class.java, searchers)).tell(message, self)
            }
        }
    }
}

fun main() {
    ActorSystem.create("MySystem").apply {
        actorOf(
            Props.create(
                SearchPrinter::class.java,
                setOf(
                    GoogleSearch::class.java,
                    YandexSearch::class.java,
                    BingSearch::class.java,
                    SlowYandexSearch::class.java,
                    SlowBingSearch::class.java
                )
            )
        ).apply {
            tell(SearchRequest("search"), ActorRef.noSender())
        }
    }
}