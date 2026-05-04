package com.prism.core

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiMethod
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import java.nio.file.Files
import kotlin.io.path.writeText

class PsiLocatorTest : LightJavaCodeInsightFixtureTestCase() {
    fun testLocateReturnsMethodForLineInsideJavaMethod() {
        val source = """
            class Sample {
                void ignored() {
                    int value = 1;
                }

                int target(int input) {
                    int doubled = input * 2;
                    return doubled;
                }
            }
        """.trimIndent()

        val sourceDir = Files.createTempDirectory("psi-locator-test")
        try {
            val sourcePath = sourceDir.resolve("Sample.java")
            sourcePath.writeText(source)

            WriteAction.runAndWait<RuntimeException> {
                LocalFileSystem.getInstance().refreshAndFindFileByPath(sourcePath.toString())
            }

            val line = source.lineNumberContaining("int doubled")
            val element = PsiLocator.locate(project, sourcePath.toString(), line)

            assertInstanceOf(element, PsiMethod::class.java)
            assertEquals("target", (element as PsiMethod).name)
        } finally {
            FileUtil.delete(sourceDir.toFile())
        }
    }

    private fun String.lineNumberContaining(marker: String): Int =
        lines().indexOfFirst { marker in it }.takeIf { it >= 0 }?.plus(1)
            ?: error("Missing marker: $marker")
}
