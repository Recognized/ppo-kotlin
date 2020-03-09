package actor

import actors.*
import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.javadsl.TestKit
import kotlinx.coroutines.delay
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.time.Duration

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

class ActorTest  {

    @Test
    fun `test all providers fast`() {
        doTest(setOf(YandexSearch::class.java, GoogleSearch::class.java, BingSearch::class.java)) {

            within(Duration.ofSeconds(3)) {
                val result: TotalResult = expectMsgAnyClassOf(TotalResult::class.java)
                assert(result.results.size == 3)

                expectNoMessage()
            }
        }
    }

    @Test
    fun `test not enough providers`() {
        doTest(setOf(GoogleSearch::class.java, BingSearch::class.java)) {

            within(Duration.ofSeconds(3)) {
                val result: TotalResult = expectMsgAnyClassOf(TotalResult::class.java)
                assert(result.results.size == 2)

                expectNoMessage()
            }
        }
    }

    @Test
    fun `test some providers are slow`() {
        doTest(setOf(GoogleSearch::class.java, SlowBingSearch::class.java, SlowYandexSearch::class.java)) {
            within(Duration.ofSeconds(3)) {
                val result: TotalResult = expectMsgAnyClassOf(TotalResult::class.java)
                assert(result.results.size == 1)

                expectNoMessage()
            }
        }
    }

    @Test
    fun `test all providers are slow`() {
        doTest(setOf(SlowBingSearch::class.java, SlowYandexSearch::class.java)) {
            within(Duration.ofSeconds(3)) {
                val result: TotalResult = expectMsgAnyClassOf(TotalResult::class.java)
                assert(result.results.isEmpty())

                expectNoMessage()
            }
        }
    }

    private fun doTest(classes: Set<Class<out SearchConcrete>>, block: TestKit.() -> Unit) {
        with(TestKit(system)) {
            val searcher = Props.create(SearchSupervisor::class.java, classes)
            val actor = childActorOf(searcher)
            actor.tell(SearchRequest("test"), ref)
            block()
        }
    }

    companion object {
        private val system = ActorSystem.create()

        @AfterAll
        fun tearDown() {
            TestKit.shutdownActorSystem(system)
        }
    }
}