package me.haroldmartin.detektrules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * Reports usages of `GlobalScope`. Coroutines launched in `GlobalScope` are not bound to any
 * lifecycle, so they leak work, are not cancelled on failure, and swallow crashes. Prefer a
 * structured `CoroutineScope`, e.g. `viewModelScope` or `lifecycleScope` on Android.
 *
 * <noncompliant>
 * GlobalScope.launch { doWork() }
 * </noncompliant>
 *
 * <compliant>
 * scope.launch { doWork() }
 * </compliant>
 */
class AvoidGlobalScope(config: Config) : Rule(config) {
    override val issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "GlobalScope coroutines are unstructured, prefer a lifecycle-aware CoroutineScope.",
        debt = Debt.TWENTY_MINS,
    )

    override fun visitReferenceExpression(expression: KtReferenceExpression) {
        super.visitReferenceExpression(expression)
        if (
            (expression as? KtNameReferenceExpression)?.getReferencedName() == "GlobalScope" &&
            expression.getStrictParentOfType<KtImportDirective>() == null
        ) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(expression),
                    message = "Avoid GlobalScope, prefer a lifecycle-aware CoroutineScope.",
                ),
            )
        }
    }
}
