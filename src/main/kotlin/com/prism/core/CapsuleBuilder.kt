package com.prism.core

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
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
    suspend fun build(project: Project, filePath: String, line: Int, budget: Int = 2000): String {
        val resolvedPath = resolveFilePath(project, filePath)
        val buildData = readAction {
            val naiveTokens = estimateNaiveTokens(project, resolvedPath)
            val target = PsiLocator.locate(project, resolvedPath.toString(), line)
                ?: return@readAction CapsuleBuildData(
                    sections = emptyList(),
                    naiveTokens = naiveTokens,
                )
            val backend = backendFor(target.containingFile)
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

        val tokens = buildData.sections.sumOf { it.tokens }
        val omitted = if (buildData.sections.isEmpty()) {
            listOf(OmittedSection(SectionKind.TARGET, "target not found or unsupported file"))
        } else {
            emptyList()
        }

        val capsule = renderer.toJson(
            sections = buildData.sections,
            stats = CapsuleStats(
                tokens = tokens,
                budget = budget,
                naiveTokens = buildData.naiveTokens,
                savedPct = savedPct(tokens, buildData.naiveTokens),
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

    private fun estimateNaiveTokens(project: Project, filePath: Path): Int {
        val virtualFile = LocalFileSystem.getInstance().findFileByNioFile(filePath)
            ?: return 0
        if (!virtualFile.isValid || !virtualFile.isInLocalFileSystem) {
            return 0
        }

        val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
            ?: return 0
        return estimator.estimate(psiFile.text)
    }

    private fun savedPct(tokens: Int, naiveTokens: Int): Double =
        if (naiveTokens == 0) {
            0.0
        } else {
            ((naiveTokens - tokens).coerceAtLeast(0).toDouble() / naiveTokens) * 100.0
        }

    private data class CapsuleBuildData(
        val sections: List<Section>,
        val naiveTokens: Int,
    )
}
