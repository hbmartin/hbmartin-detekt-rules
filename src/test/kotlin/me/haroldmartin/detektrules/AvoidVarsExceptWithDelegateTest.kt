package me.haroldmartin.detektrules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
internal class AvoidVarsExceptWithDelegateTest(private val env: KotlinCoreEnvironment) {
    @Test
    fun `reports on vars with unknown delegates`() {
        val code = """
        val shouldNotError = "hi"
        var shouldError = "hi"
        var delegated by remember { mutableStateOf(default) }
        var delegatedUnknown by notInDefaultDelegate { mutableStateOf(default) }
        """
        val findings = AvoidVarsExceptWithDelegate(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 2
        findings[0].message shouldBe "Property shouldError is a `var`iable, please make it a val."
        findings[1].message shouldBe "Property delegatedUnknown is a delegated `var`iable but the " +
            "delegate is not allowed. Change to val or configure allowed delegates regex list"
    }

    @Test
    fun `does not report on vars with configured delegates`() {
        val code = """
        var delegatedUnknown by notInDefaultDelegate { mutableStateOf(default) }
        """
        val findings = AvoidVarsExceptWithDelegate(
            config = KeyedConfig("allowedDelegates", listOf("notInDefaultDelegate")),
        ).compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }
}
