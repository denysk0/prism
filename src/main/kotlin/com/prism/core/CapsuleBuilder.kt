package com.prism.core

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.prism.backend.JavaBackend
import com.prism.backend.LanguageBackend
import com.prism.backend.Section
import com.prism.backend.SectionKind
import java.nio.file.Path

class CapsuleBuilder(
    private val estimator: TokenEstimator = CharsBy4Estimator,
    private val renderer: CapsuleRenderer = CapsuleRenderer,
) {
    suspend fun build(project: Project, filePath: String, line: Int, budget: Int = 2000): String {
        val resolvedPath = resolveFilePath(project, filePath)
        val buildData = if (!isInsideProject(project, resolvedPath)) {
            CapsuleBuildData(sections = emptyList(), naiveTokens = UNAVAILABLE_NAIVE_TOKENS)
        } else {
            readAction {
                val located = PsiLocator.locate(project, resolvedPath.toString(), line)
                    ?: return@readAction CapsuleBuildData(
                        sections = emptyList(),
                        naiveTokens = UNAVAILABLE_NAIVE_TOKENS,
                    )
                val naiveTokens = estimateNaiveTokens(located.psiFile)
                val target = located.element
                    ?: return@readAction CapsuleBuildData(
                        sections = emptyList(),
                        naiveTokens = naiveTokens,
                    )
                val backend = backendFor(located.psiFile)
                    ?: return@readAction CapsuleBuildData(
                        sections = emptyList(),
                        naiveTokens = naiveTokens,
                    )

                CapsuleBuildData(
                    sections = listOfNotNull(
                        backend.extractTarget(target),
                        backend.extractOwningClassSkeleton(target),
                    ),
                    naiveTokens = naiveTokens,
                )
            }
        }

        val (sections, budgetOmitted) = fitBudget(buildData.sections, budget)
        val tokens = sections.sumOf { it.tokens }
        val omitted = buildList {
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
        when (psiFile?.language?.id) {
            "JAVA" -> JavaBackend(estimator)
            else -> null
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
        val naiveTokens: Int,
    )

    private companion object {
        const val UNAVAILABLE_NAIVE_TOKENS = -1
    }
}
