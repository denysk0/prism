package com.prism.backend

import com.intellij.psi.PsiElement
import com.prism.core.OmittedSection

data class BackendResult(
    val sections: List<Section>,
    val omitted: List<OmittedSection> = emptyList(),
)

interface LanguageBackend {
    fun extractTarget(element: PsiElement): Section?

    fun extractOwningClassSkeleton(element: PsiElement): Section?

    fun extractCallees(element: PsiElement): BackendResult = BackendResult(emptyList())

    fun extractCallers(element: PsiElement): BackendResult = BackendResult(emptyList())

    fun extractRelevantTypes(element: PsiElement): BackendResult = BackendResult(emptyList())
}
