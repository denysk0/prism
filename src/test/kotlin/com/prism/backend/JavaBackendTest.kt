package com.prism.backend

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiClass
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

    fun testExtractOwningClassSkeletonOmitsMethodBodies() {
        val file = myFixture.configureByText(
            "Sample.java",
            """
                class Sample {
                    private int value = 7;

                    int helper() {
                        return value + 1;
                    }

                    int selected() {
                        return value + 2;
                    }
                }
            """.trimIndent(),
        )
        val method = PsiTreeUtil.findChildrenOfType(file, PsiMethod::class.java)
            .single { it.name == "selected" }

        val section = JavaBackend().extractOwningClassSkeleton(method)

        assertNotNull(section)
        assertEquals(SectionKind.OWNING_SKELETON, section!!.kind)
        assertTrue(section.text.contains("class Sample"))
        assertTrue(section.text.contains("private int value = 7;"))
        assertTrue(section.text.contains("int helper()"))
        assertTrue(section.text.contains("{ /* body omitted */ }"))
        assertFalse(section.text.contains("return value + 1"))
        assertFalse(section.text.contains("return value + 2"))
        assertFalse(section.text.contains("int selected()"))
    }

    fun testExtractOwningClassSkeletonReturnsNullForClassTarget() {
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
        val psiClass = PsiTreeUtil.findChildOfType(file, PsiClass::class.java)

        assertNull(JavaBackend().extractOwningClassSkeleton(psiClass!!))
    }
}
