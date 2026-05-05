package com.prism.mcp

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.project
import com.prism.core.CapsuleBuilder
import com.prism.core.JtokkitEstimator
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.coroutines.coroutineContext

class GetContextCapsuleToolset : McpToolset {
    @McpTool
    @McpDescription("Builds a task-scoped context capsule for a source location.")
    suspend fun get_context_capsule(
        @McpDescription("Absolute path or path relative to the project root")
        filePath: String,
        @McpDescription("1-based line number")
        line: Int,
        @McpDescription("Token budget for the capsule")
        budget: Int = 2000,
    ): String {
        if (filePath.isBlank()) return errorJson("filePath must not be blank")
        if (line < 1) return errorJson("line must be >= 1")
        if (budget <= 0) return errorJson("budget must be > 0")

        return CapsuleBuilder(estimator = sharedEstimator).build(
            project = coroutineContext.project,
            filePath = filePath,
            line = line,
            budget = budget,
        )
    }

    private fun errorJson(message: String): String =
        Json.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(),
            buildJsonObject { put("error", message) },
        )

    companion object {
        private val sharedEstimator: JtokkitEstimator by lazy { JtokkitEstimator() }
    }
}
