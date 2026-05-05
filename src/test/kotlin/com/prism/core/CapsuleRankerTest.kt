package com.prism.core

import com.prism.backend.Section
import com.prism.backend.SectionKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CapsuleRankerTest {
    @Test
    fun `fit handles empty sections`() {
        val result = CapsuleRanker.fit(emptyList(), budget = 100)

        assertTrue(result.included.isEmpty())
        assertTrue(result.omitted.isEmpty())
    }

    @Test
    fun `fit includes all sections when budget allows`() {
        val sections = listOf(
            section(SectionKind.TARGET, tokens = 10),
            section(SectionKind.CALLERS, tokens = 5),
        )

        val result = CapsuleRanker.fit(sections, budget = 20)

        assertEquals(sections, result.included)
        assertTrue(result.omitted.isEmpty())
    }

    @Test
    fun `fit omits lower priority sections when budget is partial`() {
        val target = section(SectionKind.TARGET, tokens = 10)
        val skeleton = section(SectionKind.OWNING_SKELETON, tokens = 9)
        val callers = section(SectionKind.CALLERS, tokens = 9)

        val result = CapsuleRanker.fit(listOf(callers, skeleton, target), budget = 20)

        assertEquals(listOf(target, skeleton), result.included)
        assertEquals(listOf(OmittedSection(SectionKind.CALLERS, "budget exceeded")), result.omitted)
    }

    @Test
    fun `fit keeps highest priority section when none fit`() {
        val target = section(SectionKind.TARGET, tokens = 100)
        val callers = section(SectionKind.CALLERS, tokens = 100)

        val result = CapsuleRanker.fit(listOf(callers, target), budget = 10)

        assertEquals(listOf(target), result.included)
        assertEquals(listOf(OmittedSection(SectionKind.CALLERS, "budget exceeded")), result.omitted)
    }

    @Test
    fun `fit degrades owning skeleton before omitting it`() {
        val target = section(SectionKind.TARGET, text = "target", tokens = 20)
        val skeleton = section(
            SectionKind.OWNING_SKELETON,
            text = """
                public class Sample {
                    private int secret;
                    public int visible() { /* body omitted */ }
                    private int hidden() { /* body omitted */ }
                }
            """.trimIndent(),
            tokens = 100,
        )

        val result = CapsuleRanker.fit(listOf(target, skeleton), budget = 55)
        val includedSkeleton = result.included.single { section -> section.kind == SectionKind.OWNING_SKELETON }

        assertTrue(result.omitted.isEmpty())
        assertTrue(includedSkeleton.text.contains("public int visible()"))
        assertFalse(includedSkeleton.text.contains("private int secret"))
        assertFalse(includedSkeleton.text.contains("private int hidden()"))
        assertTrue(target.tokens + includedSkeleton.tokens <= 55)
    }

    @Test
    fun `fit omits owning skeleton when reduced skeleton cannot fit`() {
        val target = section(SectionKind.TARGET, text = "target", tokens = 40)
        val skeleton = section(
            SectionKind.OWNING_SKELETON,
            text = """
                public class SampleWithVeryLongNameThatKeepsSkeletonLarge {
                    public int visible() { /* body omitted */ }
                }
            """.trimIndent(),
            tokens = 100,
        )

        val result = CapsuleRanker.fit(listOf(target, skeleton), budget = 41)

        assertEquals(listOf(target), result.included)
        assertEquals(listOf(OmittedSection(SectionKind.OWNING_SKELETON, "budget exceeded")), result.omitted)
    }

    private fun section(
        kind: SectionKind,
        text: String = kind.name,
        tokens: Int,
    ): Section =
        Section(kind = kind, text = text, tokens = tokens)
}
