package com.prism.core

import com.prism.backend.Section
import com.prism.backend.SectionKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class CapsuleStats(
    val tokens: Int,
    val budget: Int,
    val naiveTokens: Int,
    val savedPct: Double,
)

data class OmittedSection(
    val kind: SectionKind,
    val reason: String,
)

object CapsuleRenderer {
    private val json = Json {
        prettyPrint = false
    }

    fun toMarkdown(sections: List<Section>): String =
        sections.joinToString(separator = "\n\n", postfix = if (sections.isEmpty()) "" else "\n") { section ->
            val fence = codeFenceFor(section.text)
            buildString {
                appendLine("## ${section.kind.name}")
                appendLine(fence)
                append(section.text)
                if (!section.text.endsWith('\n')) {
                    appendLine()
                }
                append(fence)
            }
        }

    fun toJson(
        sections: List<Section>,
        stats: CapsuleStats,
        omitted: List<OmittedSection> = emptyList(),
    ): String {
        val capsule = buildJsonObject {
            put("sections", JsonArray(sections.map { it.toJson() }))
            put("omitted", JsonArray(omitted.map { it.toJson() }))
            put(
                "stats",
                buildJsonObject {
                    put("tokens", stats.tokens)
                    put("budget", stats.budget)
                    put("naiveTokens", stats.naiveTokens)
                    put("savedPct", stats.savedPct)
                },
            )
        }

        return json.encodeToString(JsonObject.serializer(), capsule)
    }

    private fun Section.toJson(): JsonObject = buildJsonObject {
        put("kind", kind.name)
        put("priority", priority)
        put("text", text)
        put("tokens", tokens)
    }

    private fun OmittedSection.toJson(): JsonObject = buildJsonObject {
        put("kind", kind.name)
        put("reason", reason)
    }

    private fun codeFenceFor(text: String): String {
        val longestBacktickRun = Regex("`+").findAll(text)
            .maxOfOrNull { it.value.length }
            ?: 0
        return "`".repeat(maxOf(3, longestBacktickRun + 1))
    }
}
