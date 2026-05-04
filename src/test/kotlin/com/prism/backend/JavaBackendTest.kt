package com.prism.backend

import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

class JavaBackendTest : LightJavaCodeInsightFixtureTestCase() {
    fun testExtractTargetReturnsMethodText() {
        val file = myFixture.configureByText(
            "Sample.java",
            """
                class Sample {
                    int answer() {
                        return 42;
                    }
                }
            """.trimIndent(),
        )
        val method = PsiTreeUtil.findChildrenOfType(file, PsiMethod::class.java)
            .single { it.name == "answer" }

        val section = JavaBackend().extractTarget(method)

        assertNotNull(section)
        assertEquals(SectionKind.TARGET, section!!.kind)
        assertTrue(section.text.contains("return 42"))
    }
}
