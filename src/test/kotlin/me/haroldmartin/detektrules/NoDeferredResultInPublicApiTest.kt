package me.haroldmartin.detektrules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
internal class NoDeferredResultInPublicApiTest(private val env: KotlinCoreEnvironment) {
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
        val findings = NoDeferredResultInPublicApi(Config.empty).compileAndLintWithContext(env, code)
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
        val findings = NoDeferredResultInPublicApi(Config.empty).compileAndLintWithContext(env, code)
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
        val findings = NoDeferredResultInPublicApi(Config.empty).compileAndLintWithContext(env, code)
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
        val findings = NoDeferredResultInPublicApi(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `uses resolved types and handles nullable Deferred`() {
        val code = """
        class Deferred<T>

        fun customDeferred(): Deferred<String> = Deferred()
        fun nullableDeferred(): kotlinx.coroutines.Deferred<String>? = null
        """
        val findings = NoDeferredResultInPublicApi(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `does not report overridden Deferred declarations`() {
        val code = """
        import kotlinx.coroutines.Deferred

        interface Api {
            fun fetch(): Deferred<String>
            val pending: Deferred<String>
        }

        class Repository : Api {
            override fun fetch(): Deferred<String> = TODO()
            override val pending: Deferred<String> = TODO()
        }
        """
        val findings = NoDeferredResultInPublicApi(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 2
    }

    @Test
    fun `does not report inferred non Deferred or local function results`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Deferred
        import kotlinx.coroutines.async

        class Repository(private val scope: CoroutineScope) {
            fun inferredDeferred() = scope.async { "thing" }
            val inferredDeferred = scope.async { "thing" }
            fun immediate(): String = "thing"
            val immediate: String = "thing"

            fun awaitLocally() {
                fun localDeferred(): Deferred<String> = scope.async { "thing" }
                localDeferred()
            }
        }
        """
        val findings = NoDeferredResultInPublicApi(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }
}
