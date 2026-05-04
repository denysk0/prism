package com.prism.mcp

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.project
import com.prism.core.CapsuleBuilder
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
    ): String =
        CapsuleBuilder().build(
            project = coroutineContext.project,
            filePath = filePath,
            line = line,
            budget = budget,
        )
}
