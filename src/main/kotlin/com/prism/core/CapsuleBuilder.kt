package com.prism.core

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.prism.backend.JavaBackend
import com.prism.backend.LanguageBackend
import com.prism.backend.Section
import com.prism.backend.SectionKind
import java.nio.file.Path

class CapsuleBuilder(
    private val estimator: TokenEstimator = CharsBy4Estimator,
    private val renderer: CapsuleRenderer = CapsuleRenderer,
) {
    fun build(project: Project, filePath: String, line: Int, budget: Int = 2000): String {
        val resolvedPath = resolveFilePath(project, filePath)
        val sections = ApplicationManager.getApplication().runReadAction(
            Computable {
                val target = PsiLocator.locate(project, resolvedPath, line)
                    ?: return@Computable emptyList<Section>()
                val backend = backendFor(target.containingFile)
                    ?: return@Computable emptyList<Section>()

                listOfNotNull(
                    backend.extractTarget(target),
                    backend.extractOwningClassSkeleton(target),
                )
            },
        )

        val tokens = sections.sumOf { it.tokens }
        val naiveTokens = estimateNaiveTokens(project, resolvedPath)
        val omitted = if (sections.isEmpty()) {
            listOf(OmittedSection(SectionKind.TARGET, "target not found or unsupported file"))
        } else {
            emptyList()
        }

        val capsule = renderer.toJson(
            sections = sections,
            stats = CapsuleStats(
                tokens = tokens,
                budget = budget,
                naiveTokens = naiveTokens,
                savedPct = savedPct(tokens, naiveTokens),
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

    private fun resolveFilePath(project: Project, filePath: String): String {
        val path = Path.of(filePath)
        if (path.isAbsolute) {
            return path.normalize().toString()
        }

        val basePath = project.basePath ?: return path.normalize().toString()
        return Path.of(basePath).resolve(path).normalize().toString()
    }

    private fun estimateNaiveTokens(project: Project, filePath: String): Int =
        ApplicationManager.getApplication().runReadAction(
            Computable {
                val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath)
                    ?: return@Computable 0
                val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                    ?: return@Computable 0
                estimator.estimate(psiFile.text)
            },
        )

    private fun savedPct(tokens: Int, naiveTokens: Int): Double =
        if (naiveTokens == 0) {
            0.0
        } else {
            ((naiveTokens - tokens).coerceAtLeast(0).toDouble() / naiveTokens) * 100.0
        }
}
