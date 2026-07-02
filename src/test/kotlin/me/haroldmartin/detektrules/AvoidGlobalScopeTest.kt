package me.haroldmartin.detektrules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.compileAndLint
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test

internal class AvoidGlobalScopeTest {
    @Test
    fun `reports GlobalScope usage but not its import`() {
        val code = """
        import kotlinx.coroutines.GlobalScope
        import kotlinx.coroutines.launch

        fun launching() {
            GlobalScope.launch {
                println("hi")
            }
        }
        """
        val findings = AvoidGlobalScope(Config.empty).compileAndLint(code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports fully qualified GlobalScope usage`() {
        val code = """
        import kotlinx.coroutines.launch

        fun launching() {
            kotlinx.coroutines.GlobalScope.launch {
                println("hi")
            }
        }
        """
        val findings = AvoidGlobalScope(Config.empty).compileAndLint(code)
        findings shouldHaveSize 1
    }

    @Test
    fun `does not report other scopes`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers
        import kotlinx.coroutines.launch

        fun launching(scope: CoroutineScope) {
            scope.launch {
                println("hi")
            }
        }
        """
        val findings = AvoidGlobalScope(Config.empty).compileAndLint(code)
        findings shouldHaveSize 0
    }
}
