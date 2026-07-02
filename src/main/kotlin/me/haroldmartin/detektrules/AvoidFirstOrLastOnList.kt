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
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes

/**
 * Reports calls to accessors such as `first()`, `last()`, or `single()` on a `List`, `Array`, or
 * `Sequence` since they throw an exception when no matching element is present. Prefer the
 * `*OrNull` counterparts, which return `null` instead of throwing. The set of forbidden accessors
 * is configurable via the `methods` option.
 *
 * <noncompliant>
 * val first = listOf(1, 2).first()
 * val biggest = listOf(1, 2).max()
 * </noncompliant>
 *
 * <compliant>
 * val first = listOf(1, 2).firstOrNull()
 * val biggest = listOf(1, 2).maxOrNull()
 * </compliant>
 */
@RequiresTypeResolution
class AvoidFirstOrLastOnList(config: Config) : Rule(config) {
    override val issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.Defect,
        description = "It is dangerous to call throwing accessors such as .first() or .last() on a collection " +
            "since they throw an exception if no matching element is present. " +
            "Prefer the *OrNull variants, e.g. .firstOrNull() or .lastOrNull(), instead.",
        debt = Debt.FIVE_MINS,
    )

    @Configuration("accessor methods that throw when no element matches, each of which has an *OrNull equivalent")
    private val methods: List<String> by config(DEFAULT_THROWING_ACCESSORS)

    override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
        super.visitQualifiedExpression(expression)
        val call = expression.selectorExpression as? KtCallExpression ?: return
        val name = (call.calleeExpression as? KtNameReferenceExpression)?.getReferencedName() ?: return
        if (name in methods && isThrowingReceiver(expression.receiverExpression)) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(call),
                    message = "Use .${name}OrNull${call.argumentsHint} instead of .$name${call.argumentsHint} " +
                        "in `${expression.text}`",
                ),
            )
        }
    }

    override fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression) {
        super.visitCallableReferenceExpression(expression)
        val name = expression.callableReference.getReferencedName()
        if (name in methods && isThrowingReceiver(expression.receiverExpression)) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(expression),
                    message = "Use ::${name}OrNull instead of ::$name in `${expression.text}`",
                ),
            )
        }
    }

    private fun isThrowingReceiver(receiver: KtExpression?): Boolean = receiver
        ?.let { bindingContext.getType(it) }
        ?.isThrowingAccessorReceiver == true
}

private val DEFAULT_THROWING_ACCESSORS = listOf(
    "first",
    "last",
    "single",
    "elementAt",
    "reduce",
    "reduceRight",
    "max",
    "min",
    "maxBy",
    "minBy",
)

private val RECEIVER_FQ_NAMES = setOf(
    "kotlin.collections.List",
    "kotlin.sequences.Sequence",
    "kotlin.Array",
    "kotlin.BooleanArray",
    "kotlin.ByteArray",
    "kotlin.CharArray",
    "kotlin.DoubleArray",
    "kotlin.FloatArray",
    "kotlin.IntArray",
    "kotlin.LongArray",
    "kotlin.ShortArray",
)

private val KtCallExpression.argumentsHint: String
    get() = when {
        lambdaArguments.isNotEmpty() -> " { ... }"
        valueArguments.isEmpty() -> "()"
        else -> "(...)"
    }

private val KotlinType.isThrowingAccessorReceiver: Boolean
    get() = fqNameString in RECEIVER_FQ_NAMES || supertypes().any { it.fqNameString in RECEIVER_FQ_NAMES }

private val KotlinType.fqNameString: String?
    get() = constructor.declarationDescriptor?.run { fqNameSafe.asString() }
