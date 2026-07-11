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
import io.gitlab.arturbosch.detekt.api.internal.RequiresTypeResolution
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

/**
 * Reports `String` conversions such as `toInt()` that throw `NumberFormatException` on malformed
 * input. Prefer the `*OrNull` counterparts, e.g. `toIntOrNull()`, and handle the `null` case. The
 * set of forbidden conversions is configurable via the `methods` option.
 *
 * <noncompliant>
 * val port = readLine().orEmpty().toInt()
 * </noncompliant>
 *
 * <compliant>
 * val port = readLine().orEmpty().toIntOrNull() ?: DEFAULT_PORT
 * </compliant>
 */
@RequiresTypeResolution
class AvoidToIntOrThrowingConversions(config: Config) : Rule(config) {
    override val issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.Defect,
        description = "String conversions such as .toInt() throw NumberFormatException on malformed input. " +
            "Prefer the *OrNull variants, e.g. .toIntOrNull(), instead.",
        debt = Debt.FIVE_MINS,
    )

    @Configuration("String conversion methods that throw, each of which has an *OrNull equivalent")
    private val methods: List<String> by config(DEFAULT_THROWING_CONVERSIONS)

    override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
        super.visitQualifiedExpression(expression)
        val call = expression.selectorExpression as? KtCallExpression ?: return
        val name = (call.calleeExpression as? KtNameReferenceExpression)?.getReferencedName() ?: return
        if (name in methods && isStringReceiver(expression)) {
            val argumentsHint = if (call.valueArguments.isEmpty()) "()" else "(...)"
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(call),
                    message = "Use .${name}OrNull$argumentsHint instead of .$name$argumentsHint " +
                        "in `${expression.text}`",
                ),
            )
        }
    }

    private fun isStringReceiver(expression: KtQualifiedExpression): Boolean =
        bindingContext.getType(expression.receiverExpression)
            ?.run { constructor.declarationDescriptor }
            ?.run { fqNameSafe.asString() } == "kotlin.String"
}

private val DEFAULT_THROWING_CONVERSIONS = listOf(
    "toInt",
    "toLong",
    "toShort",
    "toByte",
    "toDouble",
    "toFloat",
    "toUInt",
    "toULong",
    "toUShort",
    "toUByte",
    "toBigDecimal",
    "toBigInteger",
    "toBooleanStrict",
)
