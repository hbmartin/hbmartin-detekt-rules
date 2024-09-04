package me.haroldmartin.detektrules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest(additionalJavaSourcePaths = ["java"])
class UseNamedParametersTest(private val env: KotlinCoreEnvironment) {
    @Test
    fun `does not report builtin java function calls`() {
        val code = """
        System.out.println("test")
        println("inline test")
        """
        val findings = UseNamedParameters(ParamsConfig(0, 0)).compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `does not report java function calls`() {
        val code = """
import me.haroldmartin.detektrules.JavaTest
    fun main() {
            val testClass = JavaTest()
        testClass.something("message")
    }
        """
        val findings = UseNamedParameters(ParamsConfig(0, 0)).compileAndLintWithContext(env, code)
        findings shouldHaveSize 2
    }

    @Test
    fun `does not report static java function calls`() {
        val code = """
import me.haroldmartin.detektrules.Implementation
    fun main() {
        System.out.println("Implementation")
        val impl = Implementation()
        impl.check("msg")
}
        """
//        JavaTest.JavaStaticTest().check("msg")
        val findings = UseNamedParameters(ParamsConfig(0, 0)).compileAndLintWithContext(env, code)
        findings shouldHaveSize 2
    }

    @Test
    fun `does not report anonymous function calls`() {
        val code = """
        val anonymousFunction = fun(x: Int): Int {
            return x * 2
        }
        anonymousFunction(2)
        """
        val findings = UseNamedParameters(ParamsConfig(0, 0)).compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `does not report lambda function calls`() {
        val code = """
        val lambda = { x: Int -> x * 2 }
        lambda(2)
        """
        val findings = UseNamedParameters(ParamsConfig(0, 0)).compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `does not report functions with no callbacks`() {
        val code = """
        fun hello2(s: String, s2: String, whatever: Int, whatever2: Int) {
            System.out.println(s)
        }

        fun main() {
            hello2("test", "test2", 0, 1)
            hello2("test", s2="test2", 0, 1)
            hello2("test", "test2", whatever=0, 1)
            hello2("test", "test2", 0, whatever2=1)
            hello2(s="test", s2="test2", whatever=0, whatever2=1)
        }
        """

        val findings = UseNamedParameters(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 10
    }
}

private class ParamsConfig(private val argCountThreshold: Int, private val typeCountThreshold: Int) : Config {

    override fun subConfig(key: String): Config = this

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> valueOrNull(key: String): T? = when (key) {
        Config.ACTIVE_KEY -> true as? T
        "argCountThreshold" -> argCountThreshold as? T
        "typeCountThreshold" -> typeCountThreshold as? T
        else -> null
    }
}
