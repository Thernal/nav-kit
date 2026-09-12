package io.thernal.navkit.detektrules

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider
import io.thernal.navkit.detektrules.collections.UnsafeCollectionIndexAccess
import io.thernal.navkit.detektrules.packageboundary.LayerPackageBoundary
import io.thernal.navkit.detektrules.packageboundary.LayerPackageRequired
import io.thernal.navkit.detektrules.preview.PreviewMustBePrivate
import io.thernal.navkit.detektrules.style.ExpressionBodyNotAllowed
import io.thernal.navkit.detektrules.style.MultilineConstructorRequired

class ProjectRuleSetProvider : RuleSetProvider {
    override val ruleSetId = RuleSetId("project")

    override fun instance() = RuleSet(
        ruleSetId,
        listOf(
            ::PreviewMustBePrivate,
            ::UnsafeCollectionIndexAccess,
            ::LayerPackageBoundary,
            ::LayerPackageRequired,
            ::ExpressionBodyNotAllowed,
            ::MultilineConstructorRequired,
        ),
    )
}
