package me.haroldmartin.detektrules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.stubs.elements.KtValueArgumentElementType
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.util.getCall
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType

class UseNamedParameters(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "Prefer to use named parameters in complex function or constructor calls.",
        debt = Debt.FIVE_MINS,
    )

    private fun isKtNamedFunction(expression: KtCallExpression): Boolean =
        expression.getResolvedCall(bindingContext)?.resultingDescriptor?.source?.getPsi() is KtNamedFunction

    // TODO: gernerics
    // TODO: extension functions
    override fun visitCallExpression(expression: KtCallExpression) {
        if (!isKtNamedFunction(expression)) { return }
        println(expression.text)

//        (expression.calleeExpression as? KtNameReferenceExpression)?.let {
//            println(it.parent.text)
//        }
//        expression.getResolvedCall(bindingContext)?.let {
//            println("ResolvedCall: ${it.toString()} -> ${it::class.java}")
//        } ?: run {
//            println("ResolvedCall: no resolved call")
//        }
//        expression.getResolvedCall(bindingContext)?.resultingDescriptor?.let {
//            println("resultingDescriptor: ${it.name} -> ${it::class.java}")
//            it.dispatchReceiverParameter?.type?.let {
//                println("DispatchReceiverParameter: ${it.toString()} -> ${it::class.java}")
//            } ?: run {
//                println("DispatchReceiverParameter: no type")
//            }
//        } ?: run {
//            println("resultingDescriptor: no descriptor")
//        }
//        expression.getResolvedCall(bindingContext)?.resultingDescriptor?.source?.let {
//            println("ResolvedCall source: ${it.toString()} -> ${it::class.java}")
//
//           it.getPsi()?.let {
//                println("ResolvedCall source PSI: ${it.toString()} -> ${it::class.java}")
//            } ?: run {
//                println("ResolvedCall source PSI: no PSI")
//            }
//        } ?: run {
//            println("ResolvedCall source: no source")
//        }

        val args = expression.getResolvedCall(bindingContext)?.valueArguments ?: run {
            return
        }
        val total = args.size
        val typedArgs = args.keys.groupBy { it.type }.mapValues { (_, group) -> group.size }
        println("Total: $total , Typed: $typedArgs")
//        args.forEach { valueParamDesc, resValueArg ->
//            println("$valueParamDesc -> ${valueParamDesc.type} -> ${resValueArg}")
//        }
//
//        println(expression.getResolvedCall(bindingContext)?.typeArguments)
//        println(expression.getResolvedCall(bindingContext)?.resultingDescriptor?.typeParameters)
//        println(expression.getResolvedCall(bindingContext)?.dispatchReceiver?.type)
        println("")
    }
}
