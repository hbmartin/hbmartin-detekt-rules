package me.haroldmartin.detektrules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
internal class MutableTypeShouldBePrivateTest(private val env: KotlinCoreEnvironment) {
    @Test
    fun `reports exposed mutable type`() {
        val code = """
        import kotlinx.coroutines.flow.MutableStateFlow
        import kotlinx.coroutines.flow.asStateFlow

        class MyViewModel {
            val shouldError = MutableStateFlow(0)
            val shouldNotError = shouldError.asStateFlow()
            private val shouldNotError2 = MutableStateFlow(0)
        }
        """
        val findings = MutableTypeShouldBePrivate(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
        findings[0].message shouldBe "shouldError should be private since it is a mutable type."
    }

    @Test
    fun `does not report allowed types`() {
        val code = """
        import kotlinx.coroutines.flow.MutableSharedFlow
        import kotlinx.coroutines.flow.MutableStateFlow

        class MyViewModel {
            val shouldNotError = MutableStateFlow(0)
            val shouldNotErrorEither: MutableStateFlow<Int> = MutableStateFlow(0)
            val shouldError = MutableSharedFlow<Int>()
        }
        """
        val config = TestConfig("allowedTypes" to listOf("MutableStateFlow"))
        val findings = MutableTypeShouldBePrivate(config).compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
        findings[0].message shouldBe "shouldError should be private since it is a mutable type."
    }
}
