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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtProperty

/**
 * Reports `lateinit var` properties. Accessing a `lateinit` property before it is initialized
 * crashes with `UninitializedPropertyAccessException`, and `lateinit` also forces the property to
 * be mutable. Prefer constructor parameters, a nullable `val`, or lazy initialization. Annotations
 * that legitimately require `lateinit`, e.g. `@Inject`, can be permitted with the
 * `allowedAnnotations` option.
 *
 * <noncompliant>
 * lateinit var thing: Thing
 * </noncompliant>
 *
 * <compliant>
 * val thing: Thing by lazy { Thing() }
 * </compliant>
 */
class NoLateinitVar(config: Config) : Rule(config) {
    override val issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.Defect,
        description = "lateinit var properties can crash with UninitializedPropertyAccessException " +
            "and force mutability. Prefer constructor parameters, nullable vals, or lazy initialization.",
        debt = Debt.TWENTY_MINS,
    )

    @Configuration("annotations that permit a lateinit var, e.g. ['Inject']")
    private val allowedAnnotations: List<String> by config(emptyList<String>())
    private val allowedAnnotationShortNames: Set<String> =
        allowedAnnotations.map { it.substringAfterLast('.') }.toSet()

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        if (property.hasModifier(KtTokens.LATEINIT_KEYWORD) && !property.hasAllowedAnnotation()) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(property),
                    message = "Property ${property.name.orEmpty()} is a lateinit var, prefer a constructor " +
                        "parameter, a nullable val, or lazy initialization.",
                ),
            )
        }
    }

    private fun KtProperty.hasAllowedAnnotation(): Boolean =
        annotationEntries.any { it.shortName?.asString() in allowedAnnotationShortNames }
}
