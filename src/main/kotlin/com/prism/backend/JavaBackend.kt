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
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiType
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.PsiTreeUtil
import com.prism.core.CapsuleNavigationTarget
import com.prism.core.CharsBy4Estimator
import com.prism.core.OmittedSection
import com.prism.core.TokenEstimator

class JavaBackend(
    private val estimator: TokenEstimator = CharsBy4Estimator,
) : LanguageBackend {
    override val backendId: String = "java"

    override fun extractTarget(element: PsiElement): Section? =
        when (element) {
            is PsiMethod,
            is PsiClass,
            -> Section(
                SectionKind.TARGET,
                element.text,
                estimator.estimate(element.text),
                navigation = navigationTarget(element, javaLabel(element)),
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
            navigation = navigationTarget(owningClass, javaLabel(owningClass)),
            reduced = reducedOwningClassSkeleton(owningClass, targetMethod),
        )
    }

    override fun extractCallees(element: PsiElement): BackendResult {
        val targetMethod = findTargetMethod(element) ?: return BackendResult(emptyList())
        val body = targetMethod.body ?: return BackendResult(emptyList())
        val targetFile = targetMethod.containingFile?.virtualFile
        val seen = linkedSetOf<String>()

        val resolved = PsiTreeUtil.findChildrenOfType(body, PsiMethodCallExpression::class.java)
            .asSequence()
            .mapNotNull { call -> call.resolveMethod() }
            .filter { method -> seen.add(methodKey(method)) }
            .toList()

        val limited = resolved.take(MAX_CALLEES)
        val callees = limited.map { method ->
            val kind = if (method.containingFile?.virtualFile == targetFile) {
                SectionKind.INTERNAL_CALLEES
            } else {
                SectionKind.EXTERNAL_CALLEES
            }
            val text = calleeContext(method)
            Section(
                kind,
                text,
                estimator.estimate(text),
                navigation = navigationTarget(method, javaLabel(method)),
            )
        }

        val omitted = if (resolved.size > MAX_CALLEES) {
            listOf(
                OmittedSection(
                    omittedCalleesKind(resolved.drop(MAX_CALLEES), targetFile),
                    "cap reached: ${resolved.size - MAX_CALLEES} callees omitted",
                ),
            )
        } else {
            emptyList()
        }

        return BackendResult(callees, omitted)
    }

    override fun extractCallers(element: PsiElement): BackendResult {
        val targetMethod = findTargetMethod(element) ?: return BackendResult(emptyList())
        val project = targetMethod.project
        if (DumbService.isDumb(project)) {
            return BackendResult(
                sections = emptyList(),
                omitted = listOf(
                    OmittedSection(SectionKind.CALLERS, "dumb mode: callers unavailable until indexing completes"),
                ),
            )
        }

        val seen = linkedSetOf<String>()
        val resolved = ReferencesSearch.search(targetMethod, GlobalSearchScope.projectScope(project))
            .asSequence()
            .mapNotNull { reference -> PsiTreeUtil.getParentOfType(reference.element, PsiMethod::class.java, false) }
            .filterNot { method -> method.isEquivalentTo(targetMethod) }
            .filter { method -> seen.add(methodKey(method)) }
            .toList()

        val limited = resolved.take(MAX_CALLERS)
        val callers = limited.map { method ->
            val text = calleeContext(method)
            Section(
                SectionKind.CALLERS,
                text,
                estimator.estimate(text),
                navigation = navigationTarget(method, javaLabel(method)),
            )
        }

        val omitted = if (resolved.size > MAX_CALLERS) {
            listOf(
                OmittedSection(
                    SectionKind.CALLERS,
                    "cap reached: ${resolved.size - MAX_CALLERS} callers omitted",
                ),
            )
        } else {
            emptyList()
        }

        return BackendResult(callers, omitted)
    }

    override fun extractRelevantTypes(element: PsiElement): BackendResult {
        val targetMethod = findTargetMethod(element) ?: return BackendResult(emptyList())
        val projectFileIndex = ProjectFileIndex.getInstance(targetMethod.project)
        val seen = linkedSetOf<String>()
        val types = targetMethod.parameterList.parameters.map { parameter -> parameter.type } +
            listOfNotNull(targetMethod.returnType)

        val collected = mutableListOf<PsiClass>()
        types.forEach { type -> collectClassTypes(type, collected) }

        val sections = collected.asSequence()
            .filter { psiClass -> isProjectRelevantType(psiClass, projectFileIndex) }
            .filter { psiClass -> seen.add(classKey(psiClass)) }
            .map { psiClass ->
                val text = classSkeleton(psiClass, targetMethod = null)
                Section(
                    SectionKind.RELEVANT_TYPES,
                    text,
                    estimator.estimate(text),
                    navigation = navigationTarget(psiClass, javaLabel(psiClass)),
                )
            }
            .toList()

        return BackendResult(sections)
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

    private fun reducedOwningClassSkeleton(psiClass: PsiClass, targetMethod: PsiMethod?): Section {
        val text = buildString {
            append(classSignature(psiClass))
            append(" {\n")

            psiClass.methods
                .filterNot { method -> targetMethod != null && method.isEquivalentTo(targetMethod) }
                .filter { method -> method.hasModifierProperty(PsiModifier.PUBLIC) }
                .forEach { method -> appendMember(methodSkeleton(method)) }

            append("}")
        }
        return Section(
            SectionKind.OWNING_SKELETON,
            text,
            estimator.estimate(text).coerceAtLeast(1),
            navigation = navigationTarget(psiClass, javaLabel(psiClass)),
        )
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

    private fun omittedCalleesKind(omitted: List<PsiMethod>, targetFile: com.intellij.openapi.vfs.VirtualFile?): SectionKind {
        val kinds = omitted.map { method ->
            if (method.containingFile?.virtualFile == targetFile) {
                SectionKind.INTERNAL_CALLEES
            } else {
                SectionKind.EXTERNAL_CALLEES
            }
        }.toSet()

        return when {
            kinds.size > 1 -> SectionKind.CALLEES_PARTIAL
            kinds.singleOrNull() == SectionKind.EXTERNAL_CALLEES -> SectionKind.EXTERNAL_CALLEES
            else -> SectionKind.INTERNAL_CALLEES
        }
    }

    private fun collectClassTypes(type: PsiType?, into: MutableList<PsiClass>) {
        when (type) {
            null -> return
            is PsiClassType -> {
                type.resolve()?.let { into += it }
                type.parameters.forEach { argument -> collectClassTypes(argument, into) }
            }
            is PsiArrayType -> collectClassTypes(type.componentType, into)
            else -> Unit
        }
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

    private fun navigationTarget(element: PsiElement, label: String): CapsuleNavigationTarget =
        CapsuleNavigationTarget(
            label = label,
            pointer = SmartPointerManager.createPointer(element),
        )

    private fun javaLabel(element: PsiElement): String =
        when (element) {
            is PsiMethod -> {
                val owner = element.containingClass?.name
                if (owner.isNullOrBlank()) element.name else "$owner.${element.name}()"
            }
            is PsiClass -> element.qualifiedName ?: element.name ?: "class"
            else -> element.containingFile?.name ?: "target"
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
        const val MAX_CALLERS = 10
    }
}
