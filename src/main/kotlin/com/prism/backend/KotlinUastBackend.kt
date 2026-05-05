package com.prism.backend

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiType
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.prism.core.CapsuleNavigationTarget
import com.prism.core.OmittedSection
import com.prism.core.CharsBy4Estimator
import com.prism.core.TokenEstimator
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getUastParentOfType
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.visitor.AbstractUastVisitor

class KotlinUastBackend(
    private val estimator: TokenEstimator = CharsBy4Estimator,
) : LanguageBackend {
    override val backendId: String = "kotlin-uast"

    override fun extractTarget(element: PsiElement): Section? {
        val sourceText = findTargetSource(element)
            ?: return null
        return Section(
            SectionKind.TARGET,
            sourceText,
            estimator.estimate(sourceText),
            navigation = navigationTarget(findTargetPointerElement(element), targetLabel(element)),
        )
    }

    override fun extractOwningClassSkeleton(element: PsiElement): Section? {
        val targetMethod = findTargetMethod(element) ?: return null
        val owningClass = targetMethod.containingUClass() ?: return null
        val text = classSkeleton(owningClass, targetMethod)

        return Section(
            SectionKind.OWNING_SKELETON,
            text,
            estimator.estimate(text),
            navigation = (owningClass as UElement).sourcePsi?.let { source ->
                navigationTarget(source, owningClass.name ?: "class")
            },
            reduced = reducedOwningClassSkeleton(owningClass, targetMethod),
        )
    }

    override fun extractCallees(element: PsiElement): BackendResult {
        val targetMethod = findTargetMethod(element) ?: return BackendResult(emptyList())
        val targetFile = targetMethod.sourcePsi?.containingFile?.virtualFile
        val seen = linkedSetOf<String>()
        val resolved = mutableListOf<PsiMethod>()

        targetMethod.accept(
            object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    val method = node.resolve() ?: return false
                    if (seen.add(methodKey(method))) {
                        resolved += method
                    }
                    return false
                }
            },
        )

        val limited = resolved.take(MAX_CALLEES)
        val callees = limited.map { method ->
            val kind = if (method.containingFile?.virtualFile == targetFile) {
                SectionKind.INTERNAL_CALLEES
            } else {
                SectionKind.EXTERNAL_CALLEES
            }
            val text = methodContext(method)
            Section(
                kind,
                text,
                estimator.estimate(text),
                navigation = navigationTarget(methodPointerElement(method), methodLabel(method)),
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
        val psiMethod = targetMethod.javaPsi
        val project = psiMethod.project
        if (DumbService.isDumb(project)) {
            return BackendResult(
                sections = emptyList(),
                omitted = listOf(
                    OmittedSection(SectionKind.CALLERS, "dumb mode: callers unavailable until indexing completes"),
                ),
            )
        }

        val seen = linkedSetOf<String>()
        val callers = ReferencesSearch.search(psiMethod, GlobalSearchScope.projectScope(project))
            .asSequence()
            .mapNotNull { reference -> reference.element.getUastParentOfType<UMethod>(strict = false) }
            .filterNot { method -> method.sourcePsi == targetMethod.sourcePsi }
            .filter { method -> seen.add(uMethodKey(method)) }
            .toList()

        val limited = callers.take(MAX_CALLERS)
        val sections = limited.map { method ->
            val text = methodContext(method)
            Section(
                SectionKind.CALLERS,
                text,
                estimator.estimate(text),
                navigation = method.sourcePsi?.let { source -> navigationTarget(source, methodLabel(method)) },
            )
        }

        val omitted = if (callers.size > MAX_CALLERS) {
            listOf(
                OmittedSection(
                    SectionKind.CALLERS,
                    "cap reached: ${callers.size - MAX_CALLERS} callers omitted",
                ),
            )
        } else {
            emptyList()
        }

        return BackendResult(sections, omitted)
    }

    override fun extractRelevantTypes(element: PsiElement): BackendResult {
        val targetMethod = findTargetMethod(element) ?: return BackendResult(emptyList())
        val javaPsi = targetMethod.javaPsi
        val projectFileIndex = ProjectFileIndex.getInstance(javaPsi.project)
        val seen = linkedSetOf<String>()

        val types = targetMethod.uastParameters.map { parameter -> parameter.type } +
            listOfNotNull(targetMethod.returnType)

        val collected = mutableListOf<PsiClass>()
        types.forEach { type -> collectClassTypes(type, collected) }

        val filtered = collected.asSequence()
            .filter { psiClass -> isProjectRelevantType(psiClass, projectFileIndex) }
            .filter { psiClass -> seen.add(classKey(psiClass)) }
            .toList()

        val limited = filtered.take(MAX_RELEVANT_TYPES)
        val sections = limited.map { psiClass ->
            val text = relevantTypeSkeleton(psiClass)
            Section(
                SectionKind.RELEVANT_TYPES,
                text,
                estimator.estimate(text),
                navigation = navigationTarget(psiClass.navigationElement, psiClass.qualifiedName ?: psiClass.name ?: "class"),
            )
        }

        val omitted = if (filtered.size > MAX_RELEVANT_TYPES) {
            listOf(
                OmittedSection(
                    SectionKind.RELEVANT_TYPES,
                    "cap reached: ${filtered.size - MAX_RELEVANT_TYPES} omitted",
                ),
            )
        } else {
            emptyList()
        }

        return BackendResult(sections, omitted)
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
        if (qualifiedName != null && (
                qualifiedName.startsWith("java.") ||
                    qualifiedName.startsWith("javax.") ||
                    qualifiedName.startsWith("kotlin.") ||
                    qualifiedName.startsWith("kotlinx.")
                )
        ) {
            return false
        }

        val virtualFile = psiClass.containingFile?.virtualFile ?: return true
        return !projectFileIndex.isInLibrarySource(virtualFile)
    }

    private fun classKey(psiClass: PsiClass): String =
        psiClass.qualifiedName ?: (psiClass.containingFile?.virtualFile?.path.orEmpty() + "#" + psiClass.name)

    private fun relevantTypeSkeleton(psiClass: PsiClass): String {
        val uClass = psiClass.toUElement(UClass::class.java)
        if (uClass != null) {
            val source = (uClass as UElement).sourcePsi
            if (source != null) {
                return buildString {
                    append(classSignature(uClass))
                    append(" {\n")

                    val seenMembers = linkedSetOf<String>()
                    uClass.fields
                        .asSequence()
                        .mapNotNull { field -> (field as UElement).sourcePsi?.text }
                        .filter { text -> seenMembers.add(text) }
                        .forEach { text -> appendMember(text) }

                    uClass.methods
                        .asSequence()
                        .mapNotNull { method -> methodSkeleton(method) }
                        .filter { text -> seenMembers.add(text) }
                        .forEach { text -> appendMember(text) }

                    append("}")
                }
            }
        }

        return javaClassSkeleton(psiClass)
    }

    private fun javaClassSkeleton(psiClass: PsiClass): String {
        val text = psiClass.text ?: return psiClass.qualifiedName ?: psiClass.name.orEmpty()
        val bodyStart = psiClass.lBrace
            ?.textRange
            ?.startOffset
            ?.minus(psiClass.textRange.startOffset)
            ?: -1
        val signature = if (bodyStart >= 0) text.substring(0, bodyStart).trimEnd() else text.trimEnd()

        return buildString {
            append(signature)
            append(" {\n")

            psiClass.fields.forEach { field -> appendMember(field.text) }

            psiClass.methods.forEach { method ->
                val body = method.body
                val signatureText = if (body != null) {
                    val end = body.textRange.startOffset - method.textRange.startOffset
                    method.text.substring(0, end).trimEnd() + " { /* body omitted */ }"
                } else {
                    method.text.trimEnd()
                }
                appendMember(signatureText)
            }

            append("}")
        }
    }

    private fun findTargetSource(element: PsiElement): String? {
        val method = findTargetMethod(element)
        if (method != null) {
            return (method as UElement).sourcePsi?.text
        }

        val uClass = findTargetClass(element)
        return (uClass as UElement?)?.sourcePsi?.text
    }

    private fun findTargetMethod(element: PsiElement): UMethod? =
        element.toUElement(UMethod::class.java)
            ?: element.getUastParentOfType<UMethod>(strict = false)

    private fun findTargetClass(element: PsiElement): UClass? =
        element.toUElement(UClass::class.java)
            ?: element.getUastParentOfType<UClass>(strict = false)

    private fun findTargetPointerElement(element: PsiElement): PsiElement {
        val method = findTargetMethod(element)
        if (method != null) {
            return (method as UElement).sourcePsi ?: element
        }

        val uClass = findTargetClass(element)
        return (uClass as UElement?)?.sourcePsi ?: element
    }

    private fun UMethod.containingUClass(): UClass? {
        var current = (this as UElement).uastParent
        while (current != null) {
            if (current is UClass) {
                return current
            }
            current = current.uastParent
        }
        return null
    }

    private fun classSkeleton(uClass: UClass, targetMethod: UMethod): String =
        buildString {
            append(classSignature(uClass))
            append(" {\n")

            val seenMembers = linkedSetOf<String>()
            uClass.fields
                .asSequence()
                .mapNotNull { field -> (field as UElement).sourcePsi?.text }
                .filter { text -> seenMembers.add(text) }
                .forEach { text -> appendMember(text) }

            uClass.methods
                .asSequence()
                .filterNot { method -> method.sourcePsi == targetMethod.sourcePsi }
                .mapNotNull { method -> methodSkeleton(method) }
                .filter { text -> seenMembers.add(text) }
                .forEach { text -> appendMember(text) }

            append("}")
        }

    private fun reducedOwningClassSkeleton(uClass: UClass, targetMethod: UMethod): Section? {
        val source = (uClass as UElement).sourcePsi ?: return null
        val text = buildString {
            append(classSignature(uClass))
            append(" {\n")

            uClass.methods
                .asSequence()
                .filterNot { method -> method.sourcePsi == targetMethod.sourcePsi }
                .filter { method -> method.javaPsi.hasModifierProperty(PsiModifier.PUBLIC) }
                .mapNotNull { method -> methodSkeleton(method) }
                .forEach { text -> appendMember(text) }

            append("}")
        }
        return Section(
            SectionKind.OWNING_SKELETON,
            text,
            estimator.estimate(text).coerceAtLeast(1),
            navigation = navigationTarget(source, uClass.name ?: "class"),
        )
    }

    private fun classSignature(uClass: UClass): String {
        val text = (uClass as UElement).sourcePsi?.text ?: return uClass.name.orEmpty()
        val bodyStart = text.indexOf('{')
        return if (bodyStart >= 0) {
            text.substring(0, bodyStart).trimEnd()
        } else {
            text.trimEnd()
        }
    }

    private fun methodSkeleton(method: UMethod): String? {
        val sourceText = (method as UElement).sourcePsi?.text?.trimEnd() ?: return null
        if (!sourceText.looksLikeKotlinFunction()) {
            return null
        }

        val bodyStart = sourceText.indexOf('{')
        if (bodyStart < 0) {
            return sourceText
        }

        return sourceText.substring(0, bodyStart).trimEnd() + " { /* body omitted */ }"
    }

    private fun methodContext(method: PsiMethod): String {
        val uMethod = method.toUElement(UMethod::class.java)
        if (uMethod != null) {
            return methodContext(uMethod)
        }

        return method.text.trimEnd()
    }

    private fun navigationTarget(element: PsiElement, label: String): CapsuleNavigationTarget =
        CapsuleNavigationTarget(
            label = label,
            pointer = SmartPointerManager.createPointer(element),
        )

    private fun methodPointerElement(method: PsiMethod): PsiElement =
        method.toUElement(UMethod::class.java)?.sourcePsi ?: method

    private fun targetLabel(element: PsiElement): String {
        findTargetMethod(element)?.let { method -> return methodLabel(method) }
        findTargetClass(element)?.let { klass -> return klass.name ?: "class" }
        return element.containingFile?.name ?: "target"
    }

    private fun methodLabel(method: PsiMethod): String {
        val uMethod = method.toUElement(UMethod::class.java)
        if (uMethod != null) {
            return methodLabel(uMethod)
        }
        return method.name
    }

    private fun methodLabel(method: UMethod): String {
        val owner = method.containingUClass()?.name
        return if (owner.isNullOrBlank()) method.name else "$owner.${method.name}()"
    }

    private fun methodContext(method: UMethod): String {
        val sourceText = (method as UElement).sourcePsi?.text?.trimEnd() ?: return method.name.orEmpty()
        val bodyStart = sourceText.indexOf('{')
        if (bodyStart < 0) {
            return sourceText
        }

        val signature = sourceText.substring(0, bodyStart).trimEnd()
        val firstBodyLine = sourceText
            .substring(bodyStart + 1)
            .lineSequence()
            .map { line -> line.trim() }
            .firstOrNull { line -> line.isNotBlank() && line != "}" }

        return buildString {
            append(signature)
            if (firstBodyLine != null) {
                appendLine()
                append("// $firstBodyLine")
            }
        }
    }

    private fun methodKey(method: PsiMethod): String {
        val source = method.toUElement(UMethod::class.java)?.sourcePsi ?: method
        val file = source.containingFile?.virtualFile?.path.orEmpty()
        val range = source.textRange
        return "$file#${range.startOffset}:${range.endOffset}#${method.name}"
    }

    private fun omittedCalleesKind(
        omitted: List<PsiMethod>,
        targetFile: com.intellij.openapi.vfs.VirtualFile?,
    ): SectionKind {
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

    private fun uMethodKey(method: UMethod): String {
        val source = method.sourcePsi ?: return method.javaPsi.name
        val file = source.containingFile?.virtualFile?.path.orEmpty()
        val range = source.textRange
        return "$file#${range.startOffset}:${range.endOffset}#${method.javaPsi.name}"
    }

    private fun String.looksLikeKotlinFunction(): Boolean =
        lineSequence().any { line ->
            val trimmed = line.trimStart()
            trimmed.startsWith("fun ") ||
                trimmed.startsWith("suspend fun ") ||
                trimmed.startsWith("inline fun ") ||
                trimmed.startsWith("operator fun ") ||
                trimmed.startsWith("override fun ") ||
                trimmed.startsWith("private fun ") ||
                trimmed.startsWith("protected fun ") ||
                trimmed.startsWith("internal fun ") ||
                trimmed.startsWith("public fun ")
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
        const val MAX_RELEVANT_TYPES = 10
    }
}
