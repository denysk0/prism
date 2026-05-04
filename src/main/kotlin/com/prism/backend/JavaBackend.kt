package com.prism.backend

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.prism.core.CharsBy4Estimator
import com.prism.core.TokenEstimator

class JavaBackend(
    private val estimator: TokenEstimator = CharsBy4Estimator,
) : LanguageBackend {
    override fun extractTarget(element: PsiElement): Section? =
        when (element) {
            is PsiMethod,
            is PsiClass,
            -> Section(
                SectionKind.TARGET,
                100,
                element.text,
                estimator.estimate(element.text),
            )

            else -> null
        }

    override fun extractOwningClassSkeleton(element: PsiElement): Section? = null
}
