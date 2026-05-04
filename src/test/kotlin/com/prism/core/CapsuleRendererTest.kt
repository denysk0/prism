package com.prism.core

import com.prism.backend.Section
import com.prism.backend.SectionKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.double
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CapsuleRendererTest {
    @Test
    fun `renders sections as markdown headings and fenced code`() {
        val sections = listOf(
            Section(SectionKind.TARGET, priority = 100, text = "fun answer() = 42", tokens = 5),
            Section(SectionKind.OWNING_SKELETON, priority = 80, text = "class Service", tokens = 3),
        )

        val markdown = CapsuleRenderer.toMarkdown(sections)

        assertEquals(
            """
            ## TARGET
            ```
            fun answer() = 42
            ```

            ## OWNING_SKELETON
            ```
            class Service
            ```

            """.trimIndent(),
            markdown,
        )
    }

    @Test
    fun `uses longer markdown fence when section text contains backticks`() {
        val sections = listOf(
            Section(SectionKind.TARGET, priority = 100, text = "val code = \"```\"", tokens = 6),
        )

        val markdown = CapsuleRenderer.toMarkdown(sections)

        assertEquals(
            """
            ## TARGET
            ````
            val code = "```"
            ````

            """.trimIndent(),
            markdown,
        )
    }

    @Test
    fun `renders schema-compatible json`() {
        val sections = listOf(
            Section(SectionKind.TARGET, priority = 100, text = "println(\"hello\")", tokens = 4),
        )
        val stats = CapsuleStats(tokens = 4, budget = 100, naiveTokens = 16, savedPct = 75.0)
        val omitted = listOf(OmittedSection(SectionKind.CALLERS, "timeout"))

        val root = Json.parseToJsonElement(CapsuleRenderer.toJson(sections, stats, omitted)).jsonObject
        val renderedSection = root.getValue("sections").jsonArray.single().jsonObject
        val renderedOmitted = root.getValue("omitted").jsonArray.single().jsonObject
        val renderedStats = root.getValue("stats").jsonObject

        assertEquals("TARGET", renderedSection.getValue("kind").jsonPrimitive.content)
        assertEquals(100, renderedSection.getValue("priority").jsonPrimitive.int)
        assertEquals("println(\"hello\")", renderedSection.getValue("text").jsonPrimitive.content)
        assertEquals(4, renderedSection.getValue("tokens").jsonPrimitive.int)

        assertEquals("CALLERS", renderedOmitted.getValue("kind").jsonPrimitive.content)
        assertEquals("timeout", renderedOmitted.getValue("reason").jsonPrimitive.content)

        assertEquals(4, renderedStats.getValue("tokens").jsonPrimitive.int)
        assertEquals(100, renderedStats.getValue("budget").jsonPrimitive.int)
        assertEquals(16, renderedStats.getValue("naiveTokens").jsonPrimitive.int)
        assertEquals(75.0, renderedStats.getValue("savedPct").jsonPrimitive.double)
    }
}
