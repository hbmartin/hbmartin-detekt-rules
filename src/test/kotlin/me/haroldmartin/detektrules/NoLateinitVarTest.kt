package me.haroldmartin.detektrules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.compileAndLint
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class NoLateinitVarTest {
    @Test
    fun `reports lateinit var`() {
        val code = """
        class Screen {
            lateinit var title: String
            var subtitle: String? = null
        }
        """
        val findings = NoLateinitVar(Config.empty).compileAndLint(code)
        findings shouldHaveSize 1
        findings[0].message shouldBe "Property title is a lateinit var, prefer a constructor " +
            "parameter, a nullable val, or lazy initialization."
    }

    @Test
    fun `does not report lateinit var with allowed annotation`() {
        val code = """
        annotation class Inject

        class Screen {
            @Inject lateinit var allowed: String
            lateinit var stillReported: String
        }
        """
        val config = TestConfig("allowedAnnotations" to listOf("Inject"))
        val findings = NoLateinitVar(config).compileAndLint(code)
        findings shouldHaveSize 1
    }
}
