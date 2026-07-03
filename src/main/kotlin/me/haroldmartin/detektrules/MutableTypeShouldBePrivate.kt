package me.haroldmartin.detektrules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.config
import io.gitlab.arturbosch.detekt.api.internal.Configuration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.isPrivate

/**
 * Reports publicly exposed `Mutable*` types, e.g. `MutableStateFlow`, since mutation by consumers
 * can lead to unexpected behavior. Prefer exposing a read-only type instead, e.g. with
 * `_mutableStateFlow.asStateFlow()`. Type names matching a regex in `allowedTypes` are permitted.
 *
 * <noncompliant>
 * val state = MutableStateFlow(0)
 * </noncompliant>
 *
 * <compliant>
 * private val _state = MutableStateFlow(0)
 * val state = _state.asStateFlow()
 * </compliant>
 */
class MutableTypeShouldBePrivate(config: Config) : Rule(config) {
    override val issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.Defect,
        description = "Do not expose as public variables Mutable* types, e.g. MutableStateFlow",
        debt = Debt.FIVE_MINS,
    )

    @Configuration("regexes of mutable type names that are allowed to be exposed publicly")
    private val allowedTypes: List<Regex> by config(emptyList<String>()) {
        it.map(String::toRegex)
    }

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        if (property.isPrivate()) return
        property.guessType()?.let { type ->
            if (type.startsWith("Mutable") && !allowedTypes.any { it.matches(type.substringBefore('<')) }) {
                report(
                    CodeSmell(
                        issue = issue,
                        entity = Entity.from(property),
                        message = "${property.name ?: "Property"} should be private since it is a mutable type.",
                    ),
                )
            }
        }
    }

    // Guess type from type reference or infer it from the initializer.
    private fun KtProperty.guessType() = typeReference?.text ?: initializer?.firstChild?.text
}
