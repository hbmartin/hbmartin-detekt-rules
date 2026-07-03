package me.haroldmartin.detektrules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPostfixExpression

/**
 * Reports usage of the NPE inducing `!!` operator, prefer safe unwrapping with `?.`, `?:`, or
 * `requireNotNull`. To allow `!!` in some contexts, e.g. tests, use detekt's standard
 * `ignoreAnnotated` (e.g. `ignoreAnnotated: ['Test']`) or `excludes` (e.g. a glob matching your
 * test source sets) options on this rule.
 *
 * <noncompliant>
 * val thing: String = maybeThing!!
 * </noncompliant>
 *
 * <compliant>
 * val thing: String = maybeThing ?: "default"
 * </compliant>
 */
class NoNotNullOperator(config: Config) : Rule(config) {
    override val issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.Style,
        description = "Detects usage of NPE inducing !! operator",
        debt = Debt.FIVE_MINS,
    )

    override fun visitPostfixExpression(expression: KtPostfixExpression) {
        super.visitPostfixExpression(expression)

        if (expression.operationToken == KtTokens.EXCLEXCL) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(expression),
                    message = "Disallowed use of !! operator in expression ${expression.text}",
                ),
            )
        }
    }
}
