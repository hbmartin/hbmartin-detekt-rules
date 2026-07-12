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
internal class AvoidMutableCollectionsTest(private val env: KotlinCoreEnvironment) {
    @Test
    fun `does not report on immutable declarations`() {
        val code = """
        val immutableSet = setOf<String>()
        val immutableList = listOf<String>()
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
    fun `reports once per mutable function return type`() {
        val code = """
        fun mutableSet(): MutableSet<String> = mutableSetOf()
        fun mutableList(): MutableList<String> = mutableListOf()
        fun mutableMap(): MutableMap<String, String> = mutableMapOf()
        """
        val findings = AvoidMutableCollections(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 3
    }

    @Test
    fun `reports once per mutable property with explicit type`() {
        val code = """
        val mutableSet: MutableSet<String> = mutableSetOf()
        """
        val findings = AvoidMutableCollections(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports once when initializer is a mutable subtype of the declared type`() {
        val code = """
        val values: MutableCollection<String> = mutableListOf()
        """
        val findings = AvoidMutableCollections(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports on mutable collections within non mutable collections`() {
        val code = """
        val mutableSet = setOf(mutableSetOf<String>())
        val mutableList = listOf(mutableListOf<String>())
        val mutableMap = mapOf("test" to mutableMapOf<String, String>())
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
        // one report each for the receiver type and the return type, but not for `return this`
        val findings = AvoidMutableCollections(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 2
    }

    @Test
    fun `does not report flexible platform types from java interop`() {
        val code = """
        val env = System.getenv()
        val props = java.lang.Thread.getAllStackTraces()
        """
        val findings = AvoidMutableCollections(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `does not report inside collection builders`() {
        val code = """
        val builtList: List<Int> = buildList {
            add(1)
            this.addAll(listOf(2, 3))
        }
        val builtSet: Set<Int> = buildSet { add(1) }
        val builtMap: Map<String, Int> = buildMap { put("hi", 1) }
        """
        val findings = AvoidMutableCollections(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `reports inside non-stdlib functions named like collection builders`() {
        val code = """
        fun buildList(block: () -> Unit) = block()

        val built = buildList {
            mutableListOf(1)
        }
        """
        val findings = AvoidMutableCollections(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `does not report private or local mutable collections when allowed`() {
        val code = """
        class Things {
            private val mutableSet: MutableSet<String> = mutableSetOf()
            fun localScope(): List<String> {
                val mutableList = mutableListOf("hi")
                mutableList.add("bye")
                return mutableList.toList()
            }
        }
        """
        val config = TestConfig("allowPrivateAndLocal" to "true")
        val findings = AvoidMutableCollections(config).compileAndLintWithContext(env, code)
        findings shouldHaveSize 0
    }

    @Test
    fun `reports public mutable collections when private and local are allowed`() {
        val code = """
        class Things {
            val mutableSet: MutableSet<String> = mutableSetOf()
        }
        """
        val config = TestConfig("allowPrivateAndLocal" to "true")
        val findings = AvoidMutableCollections(config).compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports inferred public mutable collections when private and local are allowed`() {
        val code = """
        val mutableSet = mutableSetOf<String>()
        """
        val config = TestConfig("allowPrivateAndLocal" to "true")
        val findings = AvoidMutableCollections(config).compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports mutable initializer exposed as an immutable type`() {
        val code = """
        val values: Collection<Int> = mutableListOf()
        """
        val findings = AvoidMutableCollections(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports mutable collection in a constructor argument`() {
        val code = """
        open class Base(val values: List<Int>)
        class Child : Base(mutableListOf())
        """
        val findings = AvoidMutableCollections(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 1
    }

    @Test
    fun `reports mutable collection returned through an invoked lambda`() {
        val code = """
        val values = ({ mutableListOf(1) })()
        """
        val findings = AvoidMutableCollections(Config.empty).compileAndLintWithContext(env, code)
        findings shouldHaveSize 2
    }

}
