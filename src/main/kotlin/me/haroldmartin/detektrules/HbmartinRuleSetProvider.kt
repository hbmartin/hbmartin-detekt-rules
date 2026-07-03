package me.haroldmartin.detektrules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class HbmartinRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "HbmartinRuleSet"

    override fun instance(config: Config): RuleSet {
        return RuleSet(
            ruleSetId,
            listOf(
                AvoidFirstOrLastOnList(config),
                AvoidGlobalScope(config),
                AvoidMutableCollections(config),
                AvoidToIntOrThrowingConversions(config),
                AvoidVarsExceptWithDelegate(config),
                DontForceCast(config),
                MutableTypeShouldBePrivate(config),
                NoCallbacksInFunctions(config),
                NoDeferredResultInPublicApi(config),
                NoLateinitVar(config),
                NoNotNullOperator(config),
                NoRunBlocking(config),
                NoVarsInConstructor(config),
                WhenBranchSingleLineOrBraces(config),
            ),
        )
    }
}
