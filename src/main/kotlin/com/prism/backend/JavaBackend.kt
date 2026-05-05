package com.prism.backend

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.util.PsiTreeUtil
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

    override fun extractCallees(element: PsiElement): List<Section> {
        val targetMethod = findTargetMethod(element) ?: return emptyList()
        val body = targetMethod.body ?: return emptyList()
        val targetFile = targetMethod.containingFile?.virtualFile
        val seen = linkedSetOf<String>()
        val callees = mutableListOf<Section>()

        PsiTreeUtil.findChildrenOfType(body, PsiMethodCallExpression::class.java)
            .asSequence()
            .mapNotNull { call -> call.resolveMethod() }
            .filter { method -> seen.add(methodKey(method)) }
            .take(MAX_CALLEES)
            .forEach { method ->
                val kind = if (method.containingFile?.virtualFile == targetFile) {
                    SectionKind.INTERNAL_CALLEES
                } else {
                    SectionKind.EXTERNAL_CALLEES
                }
                val text = calleeContext(method)
                callees += Section(kind, text, estimator.estimate(text))
            }

        return callees
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
        return methodSignature(method) + " { /* body omitted */ }"
    }

    private fun methodSignature(method: PsiMethod): String {
        val body = method.body ?: return method.text.trimEnd()
        val signatureEnd = body.textRange.startOffset - method.textRange.startOffset
        return method.text.substring(0, signatureEnd).trimEnd()
    }

    private fun calleeContext(method: PsiMethod): String {
        val summary = docSummary(method)
        val signature = methodSignature(method)
        val firstBodyLine = method.body
            ?.statements
            ?.firstOrNull()
            ?.text
            ?.lineSequence()
            ?.firstOrNull { line -> line.isNotBlank() }
            ?.trim()

        return buildString {
            if (summary != null) {
                appendLine("/** $summary */")
            }
            append(signature)
            if (firstBodyLine != null) {
                appendLine()
                append("// $firstBodyLine")
            }
        }
    }

    private fun docSummary(method: PsiMethod): String? {
        val rawSummary = method.docComment
            ?.descriptionElements
            ?.joinToString(" ") { element -> element.text.trim() }
            ?.replace(Regex("\\s+"), " ")
            ?.trim()

        return rawSummary?.takeIf { it.isNotBlank() }
    }

    private fun methodKey(method: PsiMethod): String {
        val owner = method.containingClass?.qualifiedName ?: method.containingFile?.virtualFile?.path.orEmpty()
        val signature = method.getSignature(PsiSubstitutor.EMPTY)
        val parameters = signature.parameterTypes.joinToString(",") { type -> type.canonicalText }
        return "$owner#${signature.name}($parameters)"
    }

    private fun StringBuilder.appendMember(text: String) {
        text.trimIndent().lineSequence().forEach { line ->
            append("    ")
            append(line.trimStart())
            append("\n")
        }
        append("\n")
    }

    private companion object {
        const val MAX_CALLEES = 20
    }
}
