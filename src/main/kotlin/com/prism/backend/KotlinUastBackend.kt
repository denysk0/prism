package com.prism.backend

import com.intellij.psi.PsiElement
import com.prism.core.CharsBy4Estimator
import com.prism.core.TokenEstimator

class KotlinUastBackend(
    @Suppress("unused") private val estimator: TokenEstimator = CharsBy4Estimator,
) : LanguageBackend {
    override fun extractTarget(element: PsiElement): Section? = null

    override fun extractOwningClassSkeleton(element: PsiElement): Section? = null
}
