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
                element.text,
                estimator.estimate(element.text),
            )

            else -> null
    }

    override fun extractOwningClassSkeleton(element: PsiElement): Section? {
        if (element is PsiClass) {
            return null
        }

        val targetMethod = findTargetMethod(element)
        val owningClass = findOwningClass(element) ?: return null
        val text = buildString {
            append(classSignature(owningClass))
            append(" {\n")

            owningClass.fields.forEach { field ->
                appendMember(field.text)
            }

            owningClass.methods
                .filterNot { method -> targetMethod != null && method.isEquivalentTo(targetMethod) }
                .forEach { method ->
                    appendMember(methodSkeleton(method))
                }

            append("}")
        }

        return Section(
            SectionKind.OWNING_SKELETON,
            text,
            estimator.estimate(text),
        )
    }

    private fun findTargetMethod(element: PsiElement): PsiMethod? =
        when (element) {
            is PsiMethod -> element
            is PsiClass -> null
            else -> nearestJavaOwner(element) as? PsiMethod
        }

    private fun findOwningClass(element: PsiElement): PsiClass? =
        when (element) {
            is PsiClass -> element
            is PsiMethod -> element.containingClass
            else ->
                when (val owner = nearestJavaOwner(element)) {
                    is PsiMethod -> owner.containingClass
                    is PsiClass -> owner
                    else -> null
                }
        }

    private fun nearestJavaOwner(element: PsiElement): PsiElement? {
        var current = element.parent
        while (current != null) {
            if (current is PsiMethod || current is PsiClass) {
                return current
            }
            current = current.parent
        }

        return null
    }

    private fun classSignature(psiClass: PsiClass): String {
        val text = psiClass.text
        val bodyStart = psiClass.lBrace
            ?.textRange
            ?.startOffset
            ?.minus(psiClass.textRange.startOffset)
            ?: -1
        return if (bodyStart >= 0) {
            text.substring(0, bodyStart).trimEnd()
        } else {
            text.trimEnd()
        }
    }

    private fun methodSkeleton(method: PsiMethod): String {
        val body = method.body ?: return method.text.trimEnd()
        val signatureEnd = body.textRange.startOffset - method.textRange.startOffset
        return method.text.substring(0, signatureEnd).trimEnd() + " { /* body omitted */ }"
    }

    private fun StringBuilder.appendMember(text: String) {
        text.lineSequence().forEach { line ->
            append("    ")
            append(line)
            append("\n")
        }
        append("\n")
    }
}
