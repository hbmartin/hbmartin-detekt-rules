package me.haroldmartin.detektrules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
internal class AvoidToIntOrThrowingConversionsTest(private val env: KotlinCoreEnvironment) {
    @Test
    fun `reports throwing string conversions`() {
        val code = """
        val shouldError = "5".toInt()
        val shouldErrorAgain = "5.0".toDouble()
        val shouldNotError = "5".toIntOrNull() ?: 0
        """
        val findings = AvoidToIntOrThrowingConversions(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 2
        findings[0].message shouldContain "toIntOrNull"
    }

    @Test
    fun `reports conversion on nullable string with safe call`() {
        val code = """
        fun convert(maybeNumber: String?): Int? = maybeNumber?.toInt()
        """
        val findings = AvoidToIntOrThrowingConversions(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `does not report numeric conversions on non-string receivers`() {
        val code = """
        val shouldNotError = 5.4.toInt()
        val shouldNotErrorEither = 5L.toDouble()
        """
        val findings = AvoidToIntOrThrowingConversions(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `only reports configured methods`() {
        val code = """
        val shouldError = "5".toInt()
        val shouldNotError = "5.0".toDouble()
        """
        val config = TestConfig("methods" to listOf("toInt"))
        val findings = AvoidToIntOrThrowingConversions(config).compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `preserves an argument hint in the replacement message`() {
        val code = """
        val shouldError = "ff".toInt(16)
        """
        val findings = AvoidToIntOrThrowingConversions(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
        findings[0].message shouldContain "toIntOrNull(...)"
        findings[0].message shouldContain "toInt(...)"
    }

    @Test
    fun `does not report qualified property access`() {
        val code = """
        val shouldNotError = "five".length
        """
        val findings = AvoidToIntOrThrowingConversions(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }
}
