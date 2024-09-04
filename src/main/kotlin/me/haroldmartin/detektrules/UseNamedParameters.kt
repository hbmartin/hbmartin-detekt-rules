package me.haroldmartin.detektrules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.config
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.load.java.isFromJava
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.source.getPsi

class UseNamedParameters(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "Prefer to use named parameters in complex function or constructor calls.",
        debt = Debt.FIVE_MINS,
    )

    private val argCountThreshold: Int by config(3)

    private val typeCountThreshold: Int by config(2)

    private fun isKtNamedFunction(expression: KtCallExpression): Boolean =
        expression.getResolvedCall(bindingContext)?.resultingDescriptor?.source?.getPsi() is KtNamedFunction

    // TODO: generics
    // TODO: extension functions
    // TODO: inline functions
    // TODO: constructror
    override fun visitCallExpression(expression: KtCallExpression) {
        println(expression.text)
        println(expression.getResolvedCall(bindingContext)?.resultingDescriptor)
        (expression.getResolvedCall(bindingContext)?.resultingDescriptor)?.let {
            println("descriptor class: ${it::class.java}")
        }
        (expression.getResolvedCall(bindingContext)?.resultingDescriptor as? CallableMemberDescriptor)?.let {
            println("CallableMemberDescriptor.isFromJava: ${it.isFromJava}")
        }
        if (!isKtNamedFunction(expression)) {
            return
        }
        if (expression.valueArguments.all { it.isNamed() }) {
            return
        }
        println(expression.text)

        if (expression.valueArguments.size >= argCountThreshold) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(expression),
                    message = "${expression.name ?: "Function"} calls a function with ${expression.valueArguments.size} parameters which violates the configured threshold of $argCountThreshold.",
                ),
            )
        }

        val args = expression
            .getResolvedCall(bindingContext)
                ?.valueArguments ?: run {
                return
            }
        val typedArgsCount = args.keys.groupBy { it.type }.mapValues { (_, group) -> group.size }
        typedArgsCount.forEach { type, count ->
            if (count >= typeCountThreshold) {
                report(
                    CodeSmell(
                        issue = issue,
                        entity = Entity.from(expression),
                        message = "${expression.name ?: "Function"} calls a function with $count parameters of type $type which violates the configured threshold of $typeCountThreshold.",
                    ),
                )
            }
        }
    }
}
