package me.haroldmartin.detektrules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.isPublic

/**
 * Reports public functions and properties with an explicitly declared `Deferred` type. Returning a
 * `Deferred` leaks the concurrency implementation to callers and makes crashes hard to trace when
 * `await()` is called far from where the failure happened. Prefer a suspend function returning the
 * awaited value. Declarations with inferred types are not detected.
 *
 * <noncompliant>
 * fun fetchThing(): Deferred<Thing> = scope.async { api.getThing() }
 * </noncompliant>
 *
 * <compliant>
 * suspend fun fetchThing(): Thing = api.getThing()
 * </compliant>
 */
class NoDeferredResultInPublicApi(config: Config) : Rule(config) {
    override val issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "Public declarations should not expose Deferred results. " +
            "Prefer a suspend function returning the awaited value.",
        debt = Debt.TWENTY_MINS,
    )

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (function.isPublic && !function.isLocal && function.typeReference?.text?.isDeferredType == true) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(function),
                    message = "${function.name ?: "Function"} should return the awaited value from a suspend " +
                        "function instead of a Deferred.",
                ),
            )
        }
    }

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        if (property.isPublic && !property.isLocal && property.typeReference?.text?.isDeferredType == true) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(property),
                    message = "${property.name ?: "Property"} should not expose a Deferred, " +
                        "prefer exposing the awaited value.",
                ),
            )
        }
    }
}

private val String.isDeferredType: Boolean
    get() = this == "Deferred" ||
        startsWith("Deferred<") ||
        endsWith(".Deferred") ||
        contains(".Deferred<")
