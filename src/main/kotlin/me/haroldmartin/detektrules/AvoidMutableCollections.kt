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
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getAbbreviatedTypeOrType
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.isFlexible
import org.jetbrains.kotlin.types.typeUtil.supertypes

/**
 * Reports usages of mutable collection types, e.g. `MutableList`, since shared mutable state can
 * lead to unexpected behavior. Prefer read-only collections and functional transformations.
 * Expressions inside the standard library collection builders (`buildList`, `buildSet`, and
 * `buildMap`) are not reported, since their whole purpose is localized mutation that produces a
 * read-only collection. Set `allowPrivateAndLocal` to allow mutable collections in private
 * declarations and inside function bodies. Platform types from Java interop, e.g.
 * `(MutableList..List)`, are not reported since they are only possibly mutable.
 *
 * <noncompliant>
 * val things = mutableListOf("hi")
 * fun makeThings(): MutableList<String> = mutableListOf()
 * </noncompliant>
 *
 * <compliant>
 * val things = listOf("hi")
 * val builtThings = buildList { add("hi") }
 * </compliant>
 */
@RequiresTypeResolution
class AvoidMutableCollections(config: Config) : Rule(config) {
    override val issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.CodeSmell,
        description = "Using mutable collections can lead to unexpected behavior.",
        debt = Debt.TWENTY_MINS,
    )

    @Suppress("BooleanPropertyNaming")
    @Configuration("allow mutable collections in private declarations and local (function body) scopes")
    private val allowPrivateAndLocal: Boolean by config(false)

    override fun visitReferenceExpression(expression: KtReferenceExpression) {
        super.visitReferenceExpression(expression)
        val type = expression.getType(bindingContext) ?: return
        if (type.isMutableCollection && !expression.isCoveredOrAllowed(type)) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(expression),
                    message = "Mutable collection type $type used in ${expression.text}",
                ),
            )
        }
    }

    override fun visitTypeReference(typeReference: KtTypeReference) {
        super.visitTypeReference(typeReference)
        val type = typeReference.getAbbreviatedTypeOrType(bindingContext) ?: return
        if (type.isMutableCollection && !(allowPrivateAndLocal && typeReference.isInPrivateOrLocalScope())) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(typeReference),
                    message = "Mutable collection type $type used in ${typeReference.text}",
                ),
            )
        }
    }

    private fun KtExpression.isCoveredOrAllowed(type: KotlinType): Boolean =
        isCoveredByDeclaredType(type, bindingContext) ||
            isInsideCollectionBuilder(bindingContext) ||
            allowPrivateAndLocal && isInPrivateOrLocalScope()
}

private val BUILDER_FUNCTION_FQ_NAMES = setOf(
    "kotlin.collections.buildList",
    "kotlin.collections.buildSet",
    "kotlin.collections.buildMap",
)

// The declaration's explicit type reference already reports this type, so don't report it twice.
private fun KtExpression.isCoveredByDeclaredType(type: KotlinType, bindingContext: BindingContext): Boolean =
    getStrictParentOfType<KtCallableDeclaration>()
        ?.typeReference
        ?.getAbbreviatedTypeOrType(bindingContext)
        ?.let { declared ->
            declared.isMutableCollection && KotlinTypeChecker.DEFAULT.isSubtypeOf(type, declared)
        } == true

private fun KtExpression.isInsideCollectionBuilder(bindingContext: BindingContext): Boolean = parents
    .filterIsInstance<KtCallExpression>()
    .mapNotNull { it.calleeExpression as? KtNameReferenceExpression }
    .any { callee ->
        bindingContext[BindingContext.REFERENCE_TARGET, callee]?.run {
            fqNameSafe.asString() in BUILDER_FUNCTION_FQ_NAMES
        } == true
    }

private fun PsiElement.isInPrivateOrLocalScope(): Boolean =
    parents.filterIsInstance<KtNamedDeclaration>().any { it.isPrivate() } ||
        getStrictParentOfType<KtFunction>()?.bodyExpression?.isAncestor(this) == true

// Flexible (platform) types from Java interop are only possibly mutable, so they don't count.
private val KotlinType.isMutableCollection: Boolean
    get() = !isFlexible() &&
        (
            getKotlinTypeFqName(false).isMutableCollection ||
                supertypes()
                    .map { it.getKotlinTypeFqName(false) }
                    .any { it.isMutableCollection }
            )

private val String.isMutableCollection: Boolean
    get() = startsWith("kotlin.collections.") && contains("Mutable")
