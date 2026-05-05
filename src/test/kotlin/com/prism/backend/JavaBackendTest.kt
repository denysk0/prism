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

    fun testExtractOwningClassSkeletonNormalizesMemberIndent() {
        val file = myFixture.configureByText(
            "Sample.java",
            """
                class Sample {
                    int selected() {
                        return 42;
                    }

                    /**
                     * Helper docs.
                     */
                    @SuppressWarnings({
                      "a",
                      "b"
                    })
                    int helper() {
                        return 7;
                    }
                }
            """.trimIndent(),
        )
        val method = PsiTreeUtil.findChildrenOfType(file, PsiMethod::class.java)
            .single { it.name == "selected" }

        val section = JavaBackend().extractOwningClassSkeleton(method)

        assertNotNull(section)
        val text = section!!.text
        assertFalse(
            text,
            text.lineSequence().any { line -> Regex("^ {8,}\\S").containsMatchIn(line) },
        )
        assertTrue(text.contains("@SuppressWarnings({"))
        assertTrue(text.contains("    \"a\","))
    }

    fun testExtractCalleesReturnsResolvedDistinctMethods() {
        val file = myFixture.configureByText(
            "Sample.java",
            """
                class Sample {
                    int selected() {
                        int total = first();
                        total += second();
                        total += third();
                        total += first();
                        return total;
                    }

                    int first() {
                        return 1;
                    }

                    int second() {
                        return 2;
                    }

                    int third() {
                        return 3;
                    }
                }
            """.trimIndent(),
        )
        val method = PsiTreeUtil.findChildrenOfType(file, PsiMethod::class.java)
            .single { it.name == "selected" }

        val sections = JavaBackend().extractCallees(method)

        assertEquals(3, sections.size)
        assertTrue(sections.all { section -> section.kind == SectionKind.INTERNAL_CALLEES })
        val text = sections.joinToString("\n") { section -> section.text }
        assertTrue(text.contains("int first()"))
        assertTrue(text.contains("int second()"))
        assertTrue(text.contains("int third()"))
    }
}
