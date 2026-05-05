package com.prism.core

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.prism.backend.BackendResult
import com.prism.backend.JavaBackend
import com.prism.backend.KotlinUastBackend
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
import kotlinx.coroutines.runBlocking

class CapsuleBuilder @JvmOverloads constructor(
    private val estimator: TokenEstimator = CharsBy4Estimator,
    private val renderer: CapsuleRenderer = CapsuleRenderer,
    private val operationTimeoutMillis: Long = DEFAULT_OPERATION_TIMEOUT_MILLIS,
    private val overallTimeoutMillis: Long = DEFAULT_OVERALL_TIMEOUT_MILLIS,
    private val backendFactory: (PsiFile, TokenEstimator) -> LanguageBackend? = ::defaultBackendFor,
) {
    suspend fun build(project: Project, filePath: String, line: Int, budget: Int = 2000): String {
        val resolvedPath = resolveFilePath(project, filePath)
        val buildData = if (!isInsideProject(project, resolvedPath)) {
            CapsuleBuildData(
                sections = emptyList(),
                omitted = emptyList(),
                naiveTokens = UNAVAILABLE_NAIVE_TOKENS,
                transitiveNaiveTokens = UNAVAILABLE_NAIVE_TOKENS,
                metadata = null,
            )
        } else {
            val locatedData = readAction {
                val located = PsiLocator.locate(project, resolvedPath.toString(), line)
                    ?: return@readAction LocatedBuildData(
                        target = null,
                        backend = null,
                        psiFile = null,
                        naiveTokens = UNAVAILABLE_NAIVE_TOKENS,
                        metadata = null,
                    )
                val naiveTokens = estimateNaiveTokens(located.psiFile)
                val target = located.element
                    ?: return@readAction LocatedBuildData(
                        target = null,
                        backend = null,
                        psiFile = located.psiFile,
                        naiveTokens = naiveTokens,
                        metadata = null,
                    )
                val backend = backendFor(located.psiFile)
                    ?: return@readAction LocatedBuildData(
                        target = null,
                        backend = null,
                        psiFile = located.psiFile,
                        naiveTokens = naiveTokens,
                        metadata = null,
                    )

                LocatedBuildData(
                    target = target,
                    backend = backend,
                    psiFile = located.psiFile,
                    naiveTokens = naiveTokens,
                    metadata = CapsuleMetadata(
                        backend = backend.backendId,
                        skeletonAccuracy = backend.skeletonAccuracy,
                    ),
                )
            }
            if (locatedData.target == null || locatedData.backend == null) {
                CapsuleBuildData(
                    sections = emptyList(),
                    omitted = emptyList(),
                    naiveTokens = locatedData.naiveTokens,
                    transitiveNaiveTokens = locatedData.naiveTokens,
                    metadata = locatedData.metadata,
                )
            } else {
                val extraction = extractSections(locatedData.backend, locatedData.target)
                val transitiveNaiveTokens = readAction {
                    estimateTransitiveNaiveTokens(locatedData.psiFile, extraction.sections, locatedData.naiveTokens)
                }
                CapsuleBuildData(
                    sections = extraction.sections,
                    omitted = extraction.omitted,
                    naiveTokens = locatedData.naiveTokens,
                    transitiveNaiveTokens = transitiveNaiveTokens,
                    metadata = locatedData.metadata,
                )
            }
        }

        val fitResult = CapsuleRanker.fit(buildData.sections, budget, estimator)
        val sections = fitResult.included
        val tokens = sections.sumOf { it.tokens }
        val omitted = buildList {
            addAll(buildData.omitted)
            addAll(fitResult.omitted)
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
                transitiveNaiveTokens = buildData.transitiveNaiveTokens,
                absoluteSavedTokens = absoluteSavedTokens(tokens, buildData.naiveTokens),
            ),
            omitted = omitted,
            metadata = buildData.metadata,
        )
        val event = CapsulePublishedEvent(
            capsuleJson = capsule,
            tree = buildTree(sections),
            requestContext = CapsuleRequestContext(
                project = project,
                filePath = filePath,
                line = line,
                budget = budget,
            ),
        )
        CapsulePublicationState.getInstance(project).publish(event)
        project.messageBus.syncPublisher(CapsulePublishedTopic.TOPIC).capsulePublished(event)
        return capsule
    }

    fun buildBlocking(project: Project, filePath: String, line: Int, budget: Int = 2000): String =
        runBlocking {
            build(project, filePath, line, budget)
        }

    private fun backendFor(psiFile: PsiFile?): LanguageBackend? =
        psiFile?.let { backendFactory(it, estimator) }

    private fun extractSections(backend: LanguageBackend, target: PsiElement): OperationResult {
        val tasks = listOf(
            OperationTask(SectionKind.TARGET, "extractTarget") {
                BackendResult(listOfNotNull(backend.extractTarget(target)))
            },
            OperationTask(SectionKind.OWNING_SKELETON, "extractOwningClassSkeleton") {
                BackendResult(listOfNotNull(backend.extractOwningClassSkeleton(target)))
            },
            OperationTask(SectionKind.CALLEES_PARTIAL, "extractCallees") {
                backend.extractCallees(target)
            },
            OperationTask(SectionKind.CALLERS, "extractCallers") {
                backend.extractCallers(target)
            },
            OperationTask(SectionKind.RELEVANT_TYPES, "extractRelevantTypes") {
                backend.extractRelevantTypes(target)
            },
        )

        val futures = tasks.map { task ->
            task to CompletableFuture.supplyAsync(
                { ReadAction.compute<BackendResult, RuntimeException> { task.invoke() } },
                BACKEND_EXECUTOR,
            )
        }

        val deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(overallTimeoutMillis)
        val perOpNanos = TimeUnit.MILLISECONDS.toNanos(operationTimeoutMillis)
        val sections = mutableListOf<Section>()
        val omitted = mutableListOf<OmittedSection>()

        for ((task, future) in futures) {
            val remaining = (deadlineNanos - System.nanoTime()).coerceAtLeast(0L)
            val waitNanos = minOf(perOpNanos, remaining)
            try {
                val result = future.get(waitNanos, TimeUnit.NANOSECONDS)
                sections += result.sections
                omitted += result.omitted
            } catch (exception: Exception) {
                future.cancel(true)
                val reason = if (exception.isTimeout()) {
                    "${task.operationName}: timeout"
                } else {
                    LOG.warn("Capsule backend operation ${task.operationName} failed", exception)
                    "${task.operationName}: ${exception.rootCauseName()}"
                }
                omitted += OmittedSection(task.kind, reason)
            }
        }

        return OperationResult(sections = sections, omitted = omitted)
    }

    private data class OperationTask(
        val kind: SectionKind,
        val operationName: String,
        val invoke: () -> BackendResult,
    )

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

    private fun estimateTransitiveNaiveTokens(
        psiFile: PsiFile?,
        sections: List<Section>,
        fallbackNaiveTokens: Int,
    ): Int {
        if (fallbackNaiveTokens == UNAVAILABLE_NAIVE_TOKENS || psiFile == null) {
            return UNAVAILABLE_NAIVE_TOKENS
        }

        val files = linkedMapOf<String, PsiFile>()
        addPsiFile(files, psiFile)
        sections.asSequence()
            .filter { section ->
                section.kind == SectionKind.INTERNAL_CALLEES ||
                    section.kind == SectionKind.EXTERNAL_CALLEES ||
                    section.kind == SectionKind.RELEVANT_TYPES
            }
            .mapNotNull { section -> section.navigation?.pointer?.element?.containingFile }
            .forEach { file -> addPsiFile(files, file) }

        return files.values.sumOf { file -> estimator.estimate(file.text).coerceAtLeast(0) }
            .takeIf { tokens -> tokens > 0 }
            ?: fallbackNaiveTokens
    }

    private fun addPsiFile(files: MutableMap<String, PsiFile>, psiFile: PsiFile) {
        val key = psiFile.virtualFile?.path ?: psiFile.name
        files.putIfAbsent(key, psiFile)
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

    private fun buildTree(sections: List<Section>): CapsuleTree? {
        val target = sections.firstOrNull { section -> section.kind == SectionKind.TARGET }?.navigation ?: return null
        val groups = listOf(
            SectionKind.INTERNAL_CALLEES,
            SectionKind.EXTERNAL_CALLEES,
            SectionKind.CALLERS,
            SectionKind.RELEVANT_TYPES,
        ).mapNotNull { kind ->
            val children = sections
                .filter { section -> section.kind == kind }
                .mapNotNull { section -> section.navigation }
                .map { navigation ->
                    CapsuleTreeNode(
                        label = navigation.label,
                        pointer = navigation.pointer,
                        kind = kind,
                    )
                }

            if (children.isEmpty()) {
                null
            } else {
                CapsuleTreeNode(
                    label = kind.name,
                    pointer = null,
                    kind = kind,
                    children = children,
                )
            }
        }

        return CapsuleTree(
            CapsuleTreeNode(
                label = target.label,
                pointer = target.pointer,
                kind = SectionKind.TARGET,
                children = groups,
            ),
        )
    }

    private data class CapsuleBuildData(
        val sections: List<Section>,
        val omitted: List<OmittedSection>,
        val naiveTokens: Int,
        val transitiveNaiveTokens: Int,
        val metadata: CapsuleMetadata?,
    )

    private data class LocatedBuildData(
        val target: PsiElement?,
        val backend: LanguageBackend?,
        val psiFile: PsiFile?,
        val naiveTokens: Int,
        val metadata: CapsuleMetadata?,
    )

    private data class OperationResult(
        val sections: List<Section>,
        val omitted: List<OmittedSection>,
    )

    companion object {
        fun defaultBackendFor(psiFile: PsiFile, estimator: TokenEstimator): LanguageBackend? =
            when (psiFile.language.id) {
                "JAVA" -> JavaBackend(estimator)
                "kotlin" -> KotlinUastBackend(estimator)
                else -> null
            }

        internal const val UNAVAILABLE_NAIVE_TOKENS = -1
        internal const val DEFAULT_OPERATION_TIMEOUT_MILLIS = 2_000L
        internal const val DEFAULT_OVERALL_TIMEOUT_MILLIS = 5_000L

        @JvmStatic
        fun productionEstimatorBuilder(): CapsuleBuilder =
            CapsuleBuilder(estimator = JtokkitEstimator())

        private val LOG = Logger.getInstance(CapsuleBuilder::class.java)
        private val BACKEND_EXECUTOR = Executors.newCachedThreadPool(
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
