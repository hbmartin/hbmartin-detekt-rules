package me.haroldmartin.detektrules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

/**
 * Reports calls to `runBlocking`, which blocks the current thread until its coroutine finishes and
 * can deadlock or freeze the UI when used on a main thread. Prefer exposing suspend functions and
 * launching coroutines from a structured `CoroutineScope`. This check matches by name and so also
 * flags `runBlocking` helpers from other packages. To allow it in some contexts, e.g. tests, use
 * detekt's standard `ignoreAnnotated` or `excludes` options on this rule.
 *
 * <noncompliant>
 * fun fetchThing(): Thing = runBlocking { api.getThing() }
 * </noncompliant>
 *
 * <compliant>
 * suspend fun fetchThing(): Thing = api.getThing()
 * </compliant>
 */
class NoRunBlocking(config: Config) : Rule(config) {
    override val issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "runBlocking blocks the current thread and can deadlock or freeze the UI. " +
            "Prefer suspend functions and structured concurrency.",
        debt = Debt.TWENTY_MINS,
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val callee = expression.calleeExpression as? KtNameReferenceExpression ?: return
        if (callee.getReferencedName() == "runBlocking") {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(expression),
                    message = "Avoid runBlocking, prefer suspend functions and structured concurrency.",
                ),
            )
        }
    }
}
