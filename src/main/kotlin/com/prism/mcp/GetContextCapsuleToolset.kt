package com.prism.mcp

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool

class GetContextCapsuleToolset : McpToolset {
    @McpTool
    @McpDescription("Builds a task-scoped context capsule for a source location.")
    suspend fun get_context_capsule(): String = "hello world"
}
