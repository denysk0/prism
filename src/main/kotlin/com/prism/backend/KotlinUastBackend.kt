package com.prism.backend

import com.intellij.psi.PsiElement
import com.prism.core.CharsBy4Estimator
import com.prism.core.TokenEstimator
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getUastParentOfType
import org.jetbrains.uast.toUElement

class KotlinUastBackend(
    private val estimator: TokenEstimator = CharsBy4Estimator,
) : LanguageBackend {
    override fun extractTarget(element: PsiElement): Section? {
        val sourceText = findTargetSource(element)
            ?: return null
        return Section(
            SectionKind.TARGET,
            sourceText,
            estimator.estimate(sourceText),
        )
    }

    override fun extractOwningClassSkeleton(element: PsiElement): Section? = null

    private fun findTargetSource(element: PsiElement): String? {
        val method: UMethod? = element.toUElement(UMethod::class.java)
            ?: element.getUastParentOfType<UMethod>(strict = false)
        if (method != null) {
            return (method as UElement).sourcePsi?.text
        }

        val uClass: UClass? = element.toUElement(UClass::class.java)
            ?: element.getUastParentOfType<UClass>(strict = false)
        return (uClass as UElement?)?.sourcePsi?.text
    }
}
