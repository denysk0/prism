package com.prism.core

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.prism.backend.SectionKind
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class CapsuleBuilderTest : LightJavaCodeInsightFixtureTestCase() {
    fun testBuildReturnsTargetAndSkeletonForJavaMethod() {
        val source = """
            class Sample {
                private int value = 7;

                int helper() {
                    return value + 1;
                }

                int selected() {
                    return value + 2;
                }
            }
        """.trimIndent()
        val sourceDir = Files.createTempDirectory("capsule-builder-test")
        try {
            val sourcePath = sourceDir.resolve("Sample.java")
            sourcePath.writeText(source)
            WriteAction.runAndWait<RuntimeException> {
                LocalFileSystem.getInstance().refreshAndFindFileByPath(sourcePath.toString())
            }
            val line = source.lines().indexOfFirst { "return value + 2" in it } + 1

            val json = CapsuleBuilder().build(project, sourcePath.toString(), line, budget = 2000)
            val root = Json.parseToJsonElement(json).jsonObject
            val sections = root.getValue("sections").jsonArray
            val kinds = sections.map { it.jsonObject.getValue("kind").jsonPrimitive.content }
            val text = sections.joinToString("\n") { it.jsonObject.getValue("text").jsonPrimitive.content }

            assertEquals(listOf(SectionKind.TARGET.name, SectionKind.OWNING_SKELETON.name), kinds)
            assertTrue(text.contains("return value + 2"))
            assertTrue(text.contains("int helper()"))
            assertTrue(text.contains("{ /* body omitted */ }"))
        } finally {
            FileUtil.delete(sourceDir.toFile())
        }
    }
}
