package com.prism.core

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.prism.backend.JavaBackend
import com.prism.backend.LanguageBackend
import com.prism.backend.Section
import com.prism.backend.SectionKind
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class CapsuleBuilder(
    private val estimator: TokenEstimator = CharsBy4Estimator,
    private val renderer: CapsuleRenderer = CapsuleRenderer,
    private val operationTimeoutMillis: Long = DEFAULT_OPERATION_TIMEOUT_MILLIS,
    private val backendFactory: (PsiFile, TokenEstimator) -> LanguageBackend? = { psiFile, tokenEstimator ->
        when (psiFile.language.id) {
            "JAVA" -> JavaBackend(tokenEstimator)
            else -> null
        }
    },
) {
    suspend fun build(project: Project, filePath: String, line: Int, budget: Int = 2000): String {
        val resolvedPath = resolveFilePath(project, filePath)
        val buildData = if (!isInsideProject(project, resolvedPath)) {
            CapsuleBuildData(sections = emptyList(), omitted = emptyList(), naiveTokens = UNAVAILABLE_NAIVE_TOKENS)
        } else {
            val locatedData = readAction {
                val located = PsiLocator.locate(project, resolvedPath.toString(), line)
                    ?: return@readAction LocatedBuildData(
                        target = null,
                        backend = null,
                        psiFile = null,
                        naiveTokens = UNAVAILABLE_NAIVE_TOKENS,
                    )
                val naiveTokens = estimateNaiveTokens(located.psiFile)
                val target = located.element
                    ?: return@readAction LocatedBuildData(
                        target = null,
                        backend = null,
                        psiFile = located.psiFile,
                        naiveTokens = naiveTokens,
                    )
                val backend = backendFor(located.psiFile)
                    ?: return@readAction LocatedBuildData(
                        target = null,
                        backend = null,
                        psiFile = located.psiFile,
                        naiveTokens = naiveTokens,
                    )

                LocatedBuildData(
                    target = target,
                    backend = backend,
                    psiFile = located.psiFile,
                    naiveTokens = naiveTokens,
                )
            }
            if (locatedData.target == null || locatedData.backend == null) {
                CapsuleBuildData(sections = emptyList(), omitted = emptyList(), naiveTokens = locatedData.naiveTokens)
            } else {
                val extraction = extractSections(locatedData.backend, locatedData.target)
                CapsuleBuildData(
                    sections = extraction.sections,
                    omitted = extraction.omitted,
                    naiveTokens = locatedData.naiveTokens,
                )
            }
        }

        val (sections, budgetOmitted) = fitBudget(buildData.sections, budget)
        val tokens = sections.sumOf { it.tokens }
        val omitted = buildList {
            addAll(buildData.omitted)
            addAll(budgetOmitted)
            if (buildData.sections.isEmpty()) {
                add(OmittedSection(SectionKind.TARGET, "target not found or unsupported file"))
            }
            if (buildData.naiveTokens == UNAVAILABLE_NAIVE_TOKENS) {
                add(OmittedSection(SectionKind.TARGET, "naive baseline unavailable"))
            }
        }

        val capsule = renderer.toJson(
            sections = sections,
            stats = CapsuleStats(
                tokens = tokens,
                budget = budget,
                naiveTokens = buildData.naiveTokens,
                savedPct = savedPct(tokens, buildData.naiveTokens),
                transitiveNaiveTokens = buildData.naiveTokens,
                absoluteSavedTokens = absoluteSavedTokens(tokens, buildData.naiveTokens),
            ),
            omitted = omitted,
        )
        project.messageBus.syncPublisher(CapsulePublishedTopic.TOPIC).capsulePublished(capsule)
        return capsule
    }

    private fun backendFor(psiFile: PsiFile?): LanguageBackend? =
        psiFile?.let { backendFactory(it, estimator) }

    private fun extractSections(backend: LanguageBackend, target: PsiElement): OperationResult {
        val targetResult = runOperation(SectionKind.TARGET, "extractTarget") {
            listOfNotNull(backend.extractTarget(target))
        }
        val skeletonResult = runOperation(SectionKind.OWNING_SKELETON, "extractOwningClassSkeleton") {
            listOfNotNull(backend.extractOwningClassSkeleton(target))
        }
        val calleesResult = runOperation(SectionKind.INTERNAL_CALLEES, "extractCallees") {
            backend.extractCallees(target)
        }
        val callersResult = runOperation(SectionKind.CALLERS, "extractCallers") {
            backend.extractCallers(target)
        }
        val relevantTypesResult = runOperation(SectionKind.RELEVANT_TYPES, "extractRelevantTypes") {
            backend.extractRelevantTypes(target)
        }

        return OperationResult(
            sections = targetResult.sections +
                skeletonResult.sections +
                calleesResult.sections +
                callersResult.sections +
                relevantTypesResult.sections,
            omitted = targetResult.omitted +
                skeletonResult.omitted +
                calleesResult.omitted +
                callersResult.omitted +
                relevantTypesResult.omitted,
        )
    }

    private fun runOperation(
        omittedKind: SectionKind,
        operationName: String,
        operation: () -> List<Section>,
    ): OperationResult {
        val future = CompletableFuture.supplyAsync(
            { ReadAction.compute<List<Section>, RuntimeException> { operation() } },
            BACKEND_EXECUTOR,
        )
        return try {
            OperationResult(
                sections = future.orTimeout(operationTimeoutMillis, TimeUnit.MILLISECONDS).get(),
                omitted = emptyList(),
            )
        } catch (exception: Exception) {
            future.cancel(true)
            val reason = if (exception.isTimeout()) {
                "$operationName: timeout"
            } else {
                "$operationName: ${exception.rootCauseName()}"
            }
            OperationResult(
                sections = emptyList(),
                omitted = listOf(OmittedSection(omittedKind, reason)),
            )
        }
    }

    private fun resolveFilePath(project: Project, filePath: String): Path {
        val path = Path.of(filePath)
        if (path.isAbsolute) {
            return path.normalize()
        }

        val basePath = project.basePath ?: return path.normalize()
        return Path.of(basePath).resolve(path).normalize()
    }

    private fun isInsideProject(project: Project, filePath: Path): Boolean {
        val basePath = project.basePath ?: return true
        return filePath.normalize().startsWith(Path.of(basePath).normalize())
    }

    private fun estimateNaiveTokens(psiFile: PsiFile): Int =
        estimator.estimate(psiFile.text)
            .takeIf { it > 0 }
            ?: UNAVAILABLE_NAIVE_TOKENS

    private fun fitBudget(sections: List<Section>, budget: Int): Pair<List<Section>, List<OmittedSection>> {
        if (budget <= 0) {
            return emptyList<Section>() to sections.map { section ->
                OmittedSection(section.kind, "budget exceeded")
            }
        }

        var usedTokens = 0
        val selected = mutableListOf<Section>()
        val omitted = mutableListOf<OmittedSection>()
        sections.sortedByDescending { it.priority }.forEach { section ->
            val canFit = usedTokens + section.tokens <= budget
            if (canFit || selected.isEmpty()) {
                selected += section
                usedTokens += section.tokens
            } else {
                omitted += OmittedSection(section.kind, "budget exceeded")
            }
        }

        return selected to omitted
    }

    private fun savedPct(tokens: Int, naiveTokens: Int): Double =
        if (naiveTokens == UNAVAILABLE_NAIVE_TOKENS) {
            -1.0
        } else {
            ((naiveTokens - tokens).coerceAtLeast(0).toDouble() / naiveTokens) * 100.0
        }

    private fun absoluteSavedTokens(tokens: Int, naiveTokens: Int): Int =
        if (naiveTokens == UNAVAILABLE_NAIVE_TOKENS) {
            -1
        } else {
            (naiveTokens - tokens).coerceAtLeast(0)
        }

    private data class CapsuleBuildData(
        val sections: List<Section>,
        val omitted: List<OmittedSection>,
        val naiveTokens: Int,
    )

    private data class LocatedBuildData(
        val target: PsiElement?,
        val backend: LanguageBackend?,
        val psiFile: PsiFile?,
        val naiveTokens: Int,
    )

    private data class OperationResult(
        val sections: List<Section>,
        val omitted: List<OmittedSection>,
    )

    private companion object {
        const val UNAVAILABLE_NAIVE_TOKENS = -1
        const val DEFAULT_OPERATION_TIMEOUT_MILLIS = 2_000L

        val BACKEND_EXECUTOR = Executors.newCachedThreadPool(
            ThreadFactory { runnable ->
                Thread(runnable, "Prism Capsule Backend").apply {
                    isDaemon = true
                }
            },
        )
    }
}

private fun Exception.isTimeout(): Boolean =
    this is TimeoutException || this is CancellationException || cause is TimeoutException

private fun Exception.rootCauseName(): String {
    var current: Throwable = if (this is ExecutionException && cause != null) cause!! else this
    while (current.cause != null) {
        current = current.cause!!
    }
    return current::class.java.simpleName.ifBlank { "failed" }
}
