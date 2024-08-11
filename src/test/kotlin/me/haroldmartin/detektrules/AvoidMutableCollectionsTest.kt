package me.haroldmartin.detektrules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
internal class AvoidMutableCollectionsTest(private val env: KotlinCoreEnvironment) {
    @Test
    fun `does not report on immutable declarations`() {
        val code = """
        val immutableSet = setOf<String>()
        val immutableSet = listOf<String>()
        val immutableMap = mapOf<String, String>()
        """
        val findings = AvoidMutableCollections(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `reports on mutable declarations`() {
        val code = """
        val mutableSet = mutableSetOf<String>()
        val mutableList = mutableListOf<String>()
        val mutableMap = mutableMapOf<String, String>()
        """
        val findings = AvoidMutableCollections(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 3
        findings[0].message shouldBe
            "Mutable collection type MutableSet<String> used in mutableSetOf<String>()"
        findings[1].message shouldBe
            "Mutable collection type MutableList<String> used in mutableListOf<String>()"
        findings[2].message shouldBe "Mutable collection type MutableMap<String, String> used in " +
            "mutableMapOf<String, String>()"
    }

    @Test
    fun `does not report on immutable function return types`() {
        val code = """
        fun immutableSet(): Set<String> = setOf()
        fun immutableList(): List<String> = listOf()
        fun immutableMap(): Map<String, String> = mapOf()
        """
        val findings = AvoidMutableCollections(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `reports on mutable function return types`() {
        val code = """
        fun mutableSet(): MutableSet<String> = mutableSetOf()
        fun mutableList(): MutableList<String> = mutableListOf()
        fun mutableMap(): MutableMap<String, String> = mutableMapOf()
        """
        val findings = AvoidMutableCollections(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 6
    }

    @Test
    fun `reports on mutable collections within non mutable collections`() {
        val code = """
        val mutableSet = setOf(mutableSetOf<String>())
        val mutableList = listOf(mutableListOf<String>())
        val mutableMap = mapOf('test' to mutableMapOf<String, String>())
        """
        val findings = AvoidMutableCollections(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 3
    }

    @Test
    fun `does not report on immutable class fields`() {
        val code = """
        data class BadBadNotGood(
            val immutableSet: Set<String>,
            val immutableList: List<String>,
            val immutableMap: Map<String, String>,
        )
        """
        val findings = AvoidMutableCollections(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `reports on mutable class fields`() {
        val code = """
        data class BadBadNotGood(
            val mutableSet: MutableSet<String>,
            val mutableList: MutableList<String>,
            val mutableMap: MutableMap<String, String>,
        )
        """
        val findings = AvoidMutableCollections(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 3
    }

    @Test
    fun `reports on mutable field with class`() {
        val code = """
        data class SomeStuff(val stuff: String)
        data class BadBadNotGood(
            val otherStuff: Int,
            val mutableList: MutableList<SomeStuff>,
            val otherOtherStuff: Boolean,
        )
        """
        val findings = AvoidMutableCollections(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports on mutable return from extension function`() {
        val code = """
        private fun <T> MutableList<T>.toggle(element: T): MutableList<T> where T : Any {
            val index = indexOf(element)
            if (index != -1) {
                removeAt(index)
            } else {
                add(element)
            }
            return this
        }
        """
        val findings = AvoidMutableCollections(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 3
    }
}
