package com.prism.backend

import com.intellij.psi.PsiElement

interface LanguageBackend {
    fun extractTarget(element: PsiElement): Section?

    fun extractOwningClassSkeleton(element: PsiElement): Section?

    fun extractCallees(element: PsiElement): List<Section> = emptyList()

    fun extractCallers(element: PsiElement): List<Section> = emptyList()

    fun extractRelevantTypes(element: PsiElement): List<Section> = emptyList()
}
