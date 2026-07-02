package me.haroldmartin.detektrules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.compileAndLint
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test

internal class NoDeferredResultInPublicApiTest {
    @Test
    fun `reports public function returning Deferred`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Deferred
        import kotlinx.coroutines.async

        class Repository(private val scope: CoroutineScope) {
            fun fetchThing(): Deferred<String> = scope.async { "thing" }
        }
        """
        val findings = NoDeferredResultInPublicApi(Config.empty).compileAndLint(code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports public property with Deferred type`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Deferred
        import kotlinx.coroutines.async

        class Repository(scope: CoroutineScope) {
            val thing: Deferred<String> = scope.async { "thing" }
        }
        """
        val findings = NoDeferredResultInPublicApi(Config.empty).compileAndLint(code)
        findings shouldHaveSize 1
    }

    @Test
    fun `does not report private or internal Deferred declarations`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Deferred
        import kotlinx.coroutines.async

        class Repository(private val scope: CoroutineScope) {
            private fun fetchThing(): Deferred<String> = scope.async { "thing" }
            internal fun fetchOtherThing(): Deferred<String> = fetchThing()
            suspend fun thing(): String = fetchThing().await()
        }
        """
        val findings = NoDeferredResultInPublicApi(Config.empty).compileAndLint(code)
        findings shouldHaveSize 0
    }

    @Test
    fun `does not report local Deferred declarations`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Deferred
        import kotlinx.coroutines.async

        suspend fun compute(scope: CoroutineScope): String {
            val deferred: Deferred<String> = scope.async { "thing" }
            return deferred.await()
        }
        """
        val findings = NoDeferredResultInPublicApi(Config.empty).compileAndLint(code)
        findings shouldHaveSize 0
    }
}
