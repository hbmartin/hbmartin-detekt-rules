package me.haroldmartin.detektrules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class UseNamedParametersTest(private val env: KotlinCoreEnvironment) {
    @Test
    fun `does not report functions with no callbacks`() {
        val code = """
        fun hello(s: String) {
            println(s)
        }
        fun hello2(s: String, s2: String, whatever: Int, whatever2: Int) {
            hello(s)
        }

        fun main() {
            hello2("test", "test2", 0, 1)
            hello2("test", s2="test2", 0, 1)
            hello2("test", "test2", whatever=0, 1)
            hello2("test", "test2", 0, whatever2=1)
            hello2(s="test", s2="test2", whatever=0, whatever2=1)

            val java = JavaTestClass()
            java.javaFunction("test")
            // This is represented by KtFunction (but not KtNamedFunction)
            anonymousFunction(2)
            anonymousFunction(x = 2)
            lambda(2)
            lambda(x = 2)
        }
        """
        JavaTestClass().javaFunction("test")
        val findings = UseNamedParameters(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }
}

fun hello(s: String) {
    println(s)
}

fun hello2(s: String) {
    hello(s)
}
