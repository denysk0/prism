package com.prism.backend

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiType
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
        val text = classSkeleton(owningClass, targetMethod)

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

    override fun extractCallers(element: PsiElement): List<Section> {
        val targetMethod = findTargetMethod(element) ?: return emptyList()
        val project = targetMethod.project
        if (DumbService.isDumb(project)) {
            return emptyList()
        }

        val seen = linkedSetOf<String>()
        return ReferencesSearch.search(targetMethod, GlobalSearchScope.projectScope(project))
            .asSequence()
            .mapNotNull { reference -> PsiTreeUtil.getParentOfType(reference.element, PsiMethod::class.java, false) }
            .filterNot { method -> method.isEquivalentTo(targetMethod) }
            .filter { method -> seen.add(methodKey(method)) }
            .take(MAX_CALLERS)
            .map { method ->
                val text = calleeContext(method)
                Section(SectionKind.CALLERS, text, estimator.estimate(text))
            }
            .toList()
    }

    override fun extractRelevantTypes(element: PsiElement): List<Section> {
        val targetMethod = findTargetMethod(element) ?: return emptyList()
        val projectFileIndex = ProjectFileIndex.getInstance(targetMethod.project)
        val seen = linkedSetOf<String>()
        val types = targetMethod.parameterList.parameters.map { parameter -> parameter.type } +
            listOfNotNull(targetMethod.returnType)

        return types
            .asSequence()
            .mapNotNull(::resolveClassType)
            .filter { psiClass -> isProjectRelevantType(psiClass, projectFileIndex) }
            .filter { psiClass -> seen.add(classKey(psiClass)) }
            .map { psiClass ->
                val text = classSkeleton(psiClass, targetMethod = null)
                Section(SectionKind.RELEVANT_TYPES, text, estimator.estimate(text))
            }
            .toList()
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

    private fun classSkeleton(psiClass: PsiClass, targetMethod: PsiMethod?): String =
        buildString {
            append(classSignature(psiClass))
            append(" {\n")

            psiClass.fields.forEach { field ->
                appendMember(field.text)
            }

            psiClass.methods
                .filterNot { method -> targetMethod != null && method.isEquivalentTo(targetMethod) }
                .forEach { method ->
                    appendMember(methodSkeleton(method))
                }

            append("}")
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

    private fun resolveClassType(type: PsiType): PsiClass? =
        when (type) {
            is PsiClassType -> type.resolve()
            is PsiArrayType -> resolveClassType(type.componentType)
            else -> null
        }

    private fun isProjectRelevantType(psiClass: PsiClass, projectFileIndex: ProjectFileIndex): Boolean {
        val qualifiedName = psiClass.qualifiedName
        if (qualifiedName?.startsWith("java.") == true || qualifiedName?.startsWith("javax.") == true) {
            return false
        }

        val virtualFile = psiClass.containingFile?.virtualFile ?: return true
        return !projectFileIndex.isInLibrarySource(virtualFile)
    }

    private fun classKey(psiClass: PsiClass): String =
        psiClass.qualifiedName ?: psiClass.containingFile?.virtualFile?.path.orEmpty() + "#" + psiClass.name

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
        const val MAX_CALLERS = 10
    }
}
