package me.haroldmartin.detektrules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.compileAndLint
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test

internal class NoRunBlockingTest {
    @Test
    fun `reports runBlocking call`() {
        val code = """
        import kotlinx.coroutines.runBlocking

        fun blocking(): Int = runBlocking {
            41 + 1
        }
        """
        val findings = NoRunBlocking(Config.empty).compileAndLint(code)
        findings shouldHaveSize 1
    }

    @Test
    fun `does not report other coroutine builders`() {
        val code = """
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers
        import kotlinx.coroutines.launch

        fun launching() {
            CoroutineScope(Dispatchers.Default).launch {
                println("hi")
            }
        }
        """
        val findings = NoRunBlocking(Config.empty).compileAndLint(code)
        findings shouldHaveSize 0
    }

    @Test
    fun `does not report invoking a lambda expression`() {
        val code = """
        val answer = ({ 42 })()
        """
        val findings = NoRunBlocking(Config.empty).compileAndLint(code)
        findings shouldHaveSize 0
    }
}
