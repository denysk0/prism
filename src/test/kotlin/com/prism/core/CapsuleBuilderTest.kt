package com.prism.core

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.prism.backend.LanguageBackend
import com.prism.backend.Section
import com.prism.backend.SectionKind
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class CapsuleBuilderTest : LightJavaCodeInsightFixtureTestCase() {
    fun testBuildReturnsTargetAndSkeletonForJavaMethod() = runBlocking {
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
        val filePath = createProjectFile("Sample.java", source)
        val line = source.lines().indexOfFirst { "return value + 2" in it } + 1

        val root = buildCapsule(filePath.toString(), line)
        val sections = root.getValue("sections").jsonArray
        val kinds = sections.map { it.jsonObject.getValue("kind").jsonPrimitive.content }
        val text = sections.joinToString("\n") { it.jsonObject.getValue("text").jsonPrimitive.content }

        assertEquals(listOf(SectionKind.TARGET.name, SectionKind.OWNING_SKELETON.name), kinds)
        assertTrue(text.contains("return value + 2"))
        assertTrue(text.contains("int helper()"))
        assertTrue(text.contains("{ /* body omitted */ }"))
    }

    fun testBuildReturnsOmittedTargetForLineOutOfRange() = runBlocking {
        val filePath = createProjectFile(
            "OutOfRange.java",
            """
                class OutOfRange {
                    void target() {
                    }
                }
            """.trimIndent(),
        )

        val root = buildCapsule(filePath.toString(), line = 100)

        assertTargetOmitted(root)
    }

    fun testBuildOmitsLowerPrioritySectionsWhenBudgetIsExceeded() = runBlocking {
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
        val filePath = createProjectFile("Budget.java", source)
        val line = source.lines().indexOfFirst { "return value + 2" in it } + 1

        val root = buildCapsule(filePath.toString(), line, budget = 1)
        val sections = root.getValue("sections").jsonArray
        val omitted = root.getValue("omitted").jsonArray

        assertEquals(
            listOf(SectionKind.TARGET.name),
            sections.map { it.jsonObject.getValue("kind").jsonPrimitive.content },
        )
        assertTrue(
            omitted.any { entry ->
                entry.jsonObject.getValue("kind").jsonPrimitive.content == SectionKind.OWNING_SKELETON.name &&
                    entry.jsonObject.getValue("reason").jsonPrimitive.content == "budget exceeded"
            },
        )
    }

    fun testBuildReturnsOmittedTargetForUnsupportedFile() = runBlocking {
        val filePath = createProjectFile(
            "Sample.kt",
            """
                class Sample {
                    fun target() = 42
                }
            """.trimIndent(),
        )

        val root = buildCapsule(filePath.toString(), line = 2)

        assertTargetOmitted(root)
    }

    fun testBuildReturnsOmittedTargetForFileOutsideProject() = runBlocking {
        val sourceDir = Files.createTempDirectory("capsule-builder-outside")
        try {
            val sourcePath = sourceDir.resolve("Outside.java")
            sourcePath.writeText(
                """
                    class Outside {
                        void target() {
                        }
                    }
                """.trimIndent(),
            )

            val root = buildCapsule(sourcePath.toString(), line = 2)

            assertTargetOmitted(root)
            assertUnavailableNaiveBaseline(root)
        } finally {
            FileUtil.delete(sourceDir.toFile())
        }
    }

    fun testBuildUsesSentinelWhenNaiveBaselineIsUnavailable() = runBlocking {
        val root = buildCapsule("/definitely/not/a/project/file.java", line = 1)

        assertTargetOmitted(root)
        assertUnavailableNaiveBaseline(root)
    }

    fun testBuildRecordsTimeoutForSlowBackendOperation() = runBlocking {
        val source = """
            class SlowSample {
                int selected() {
                    return 42;
                }
            }
        """.trimIndent()
        val filePath = createProjectFile("SlowSample.java", source)
        val line = source.lines().indexOfFirst { "return 42" in it } + 1
        val builder = CapsuleBuilder(
            operationTimeoutMillis = 20,
            backendFactory = { _, _ -> SlowCalleeBackend },
        )

        val root = Json.parseToJsonElement(
            builder.build(project, filePath.toString(), line, budget = 2000),
        ).jsonObject
        val omitted = root.getValue("omitted").jsonArray
        val sections = root.getValue("sections").jsonArray

        assertTrue(sections.any { entry ->
            entry.jsonObject.getValue("kind").jsonPrimitive.content == SectionKind.TARGET.name
        })
        assertTrue(
            omitted.any { entry ->
                entry.jsonObject.getValue("kind").jsonPrimitive.content == SectionKind.INTERNAL_CALLEES.name &&
                    entry.jsonObject.getValue("reason").jsonPrimitive.content == "extractCallees: timeout"
            },
        )
    }

    private suspend fun buildCapsule(filePath: String, line: Int, budget: Int = 2000) =
        Json.parseToJsonElement(
            CapsuleBuilder().build(project, filePath, line, budget = budget),
        ).jsonObject

    private fun createProjectFile(name: String, text: String): Path {
        val root = Path.of(project.basePath!!).resolve("build/tmp/capsule-builder-test")
        Files.createDirectories(root)
        val path = root.resolve("${System.nanoTime()}-$name")
        path.writeText(text)
        WriteAction.runAndWait<RuntimeException> {
            LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path)
        }
        return path
    }

    private fun assertTargetOmitted(root: kotlinx.serialization.json.JsonObject) {
        val sections = root.getValue("sections").jsonArray
        val omitted = root.getValue("omitted").jsonArray

        assertTrue(sections.isEmpty())
        assertTrue(
            omitted.any { entry ->
                entry.jsonObject.getValue("kind").jsonPrimitive.content == SectionKind.TARGET.name
            },
        )
    }

    private fun assertUnavailableNaiveBaseline(root: kotlinx.serialization.json.JsonObject) {
        val stats = root.getValue("stats").jsonObject
        val omittedReasons = root.getValue("omitted").jsonArray
            .map { it.jsonObject.getValue("reason").jsonPrimitive.content }

        assertEquals(-1, stats.getValue("naiveTokens").jsonPrimitive.int)
        assertEquals(-1.0, stats.getValue("savedPct").jsonPrimitive.double)
        assertEquals(-1, stats.getValue("transitiveNaiveTokens").jsonPrimitive.int)
        assertEquals(-1, stats.getValue("absoluteSavedTokens").jsonPrimitive.int)
        assertTrue("naive baseline unavailable" in omittedReasons)
    }

    private object SlowCalleeBackend : LanguageBackend {
        override fun extractTarget(element: PsiElement): Section =
            Section(SectionKind.TARGET, element.text, tokens = 1)

        override fun extractOwningClassSkeleton(element: PsiElement): Section? = null

        override fun extractCallees(element: PsiElement): List<Section> {
            Thread.sleep(200)
            return listOf(Section(SectionKind.INTERNAL_CALLEES, "slow()", tokens = 1))
        }
    }
}
