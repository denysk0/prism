package com.prism.core

import com.prism.backend.Section
import com.prism.backend.SectionKind

data class FitResult(
    val included: List<Section>,
    val omitted: List<OmittedSection>,
)

object CapsuleRanker {
    fun fit(
        sections: List<Section>,
        budget: Int,
        estimator: TokenEstimator = CharsBy4Estimator,
    ): FitResult {
        if (budget <= 0) {
            return FitResult(
                included = emptyList(),
                omitted = sections.map { section -> OmittedSection(section.kind, "budget exceeded") },
            )
        }

        val degradedSections = degradeOwningSkeletonIfNeeded(sections, budget, estimator)
        var usedTokens = 0
        val included = mutableListOf<Section>()
        val omitted = mutableListOf<OmittedSection>()
        degradedSections.sortedByDescending { section -> section.priority }.forEach { section ->
            val canFit = usedTokens + section.tokens <= budget
            if (canFit || included.isEmpty()) {
                included += section
                usedTokens += section.tokens
            } else {
                omitted += OmittedSection(section.kind, "budget exceeded")
            }
        }

        return FitResult(included = included, omitted = omitted)
    }

    private fun degradeOwningSkeletonIfNeeded(
        sections: List<Section>,
        budget: Int,
        estimator: TokenEstimator,
    ): List<Section> {
        val target = sections.firstOrNull { section -> section.kind == SectionKind.TARGET }
            ?: return sections
        val skeleton = sections.firstOrNull { section -> section.kind == SectionKind.OWNING_SKELETON }
            ?: return sections
        if (target.tokens + skeleton.tokens <= budget) {
            return sections
        }

        val reducedSkeleton = skeleton.reduced ?: skeleton.reducedOwningSkeleton(estimator)
        if (target.tokens + reducedSkeleton.tokens > budget) {
            return sections
        }

        return sections.map { section ->
            if (section === skeleton) reducedSkeleton else section
        }
    }

    private fun Section.reducedOwningSkeleton(estimator: TokenEstimator): Section {
        val reducedLines = mutableListOf<String>()
        val lines = text.lines()
        val firstLine = lines.firstOrNull()
        if (firstLine != null) {
            reducedLines += firstLine
        }

        lines.asSequence()
            .map { line -> line.trim() }
            .filter { line -> line.startsWith("public ") && line.contains("{ /* body omitted */ }") }
            .map { line -> "    $line" }
            .forEach { line -> reducedLines += line }

        reducedLines += "}"
        val reducedText = reducedLines.joinToString("\n")
        return copy(
            text = reducedText,
            tokens = estimator.estimate(reducedText).coerceAtLeast(1),
        )
    }
}
