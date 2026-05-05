package com.prism.backend

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.DumbModeTestUtils
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

    fun testExtractOwningClassSkeletonProvidesReducedPublicSkeleton() {
        val file = myFixture.configureByText(
            "Sample.java",
            """
                public class Sample {
                    public Sample() {
                    }

                    public static <T> T convert(T value) {
                        return value;
                    }

                    public int selected() {
                        return 42;
                    }

                    private int hidden() {
                        return 7;
                    }
                }
            """.trimIndent(),
        )
        val method = PsiTreeUtil.findChildrenOfType(file, PsiMethod::class.java)
            .single { it.name == "selected" }

        val reduced = JavaBackend().extractOwningClassSkeleton(method)!!.reduced

        assertNotNull(reduced)
        val text = reduced!!.text
        assertTrue(text.contains("public Sample()"))
        assertTrue(text.contains("public static <T> T convert(T value)"))
        assertFalse(text.contains("public int selected()"))
        assertFalse(text.contains("private int hidden()"))
        assertFalse(text.contains("return value"))
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

        val result = JavaBackend().extractCallees(method)
        val sections = result.sections

        assertEquals(3, sections.size)
        assertTrue(sections.all { section -> section.kind == SectionKind.INTERNAL_CALLEES })
        val text = sections.joinToString("\n") { section -> section.text }
        assertTrue(text.contains("int first()"))
        assertTrue(text.contains("int second()"))
        assertTrue(text.contains("int third()"))
        assertTrue(result.omitted.isEmpty())
    }

    fun testExtractCalleesUsesCompactContext() {
        val file = myFixture.configureByText(
            "Sample.java",
            """
                class Sample {
                    int selected() {
                        return helper();
                    }

                    /**
                     * Computes the helper value.
                     */
                    int helper() {
                        int value = 7;
                        return value;
                    }
                }
            """.trimIndent(),
        )
        val method = PsiTreeUtil.findChildrenOfType(file, PsiMethod::class.java)
            .single { it.name == "selected" }

        val section = JavaBackend().extractCallees(method).sections.single()

        assertEquals(SectionKind.INTERNAL_CALLEES, section.kind)
        assertTrue(section.text.contains("Computes the helper value."))
        assertTrue(section.text.contains("int helper()"))
        assertTrue(section.text.contains("int value = 7;"))
        assertFalse(section.text.contains("return value;"))
    }

    fun testExtractCallersReturnsResolvedDistinctCallers() {
        val file = myFixture.configureByText(
            "Sample.java",
            """
                class Sample {
                    int first() {
                        return target();
                    }

                    int second() {
                        return target();
                    }

                    int duplicate() {
                        int value = target();
                        return value + target();
                    }

                    int target() {
                        return 42;
                    }
                }
            """.trimIndent(),
        )
        val target = PsiTreeUtil.findChildrenOfType(file, PsiMethod::class.java)
            .single { it.name == "target" }

        val result = JavaBackend().extractCallers(target)
        val sections = result.sections

        assertEquals(3, sections.size)
        assertTrue(sections.all { section -> section.kind == SectionKind.CALLERS })
        val text = sections.joinToString("\n") { section -> section.text }
        assertTrue(text.contains("int first()"))
        assertTrue(text.contains("int second()"))
        assertTrue(text.contains("int duplicate()"))
        assertTrue(result.omitted.isEmpty())
    }

    fun testExtractRelevantTypesReturnsProjectDtoSkeleton() {
        val file = myFixture.configureByText(
            "Sample.java",
            """
                import java.util.List;

                class Sample {
                    Receipt checkout(Order order, String accountId, List<String> tags) {
                        return new Receipt(order.id());
                    }
                }

                class Order {
                    private final String id;

                    Order(String id) {
                        this.id = id;
                    }

                    String id() {
                        return id;
                    }
                }

                class Receipt {
                    private final String orderId;

                    Receipt(String orderId) {
                        this.orderId = orderId;
                    }

                    String orderId() {
                        return orderId;
                    }
                }
            """.trimIndent(),
        )
        val method = PsiTreeUtil.findChildrenOfType(file, PsiMethod::class.java)
            .single { it.name == "checkout" }

        val sections = JavaBackend().extractRelevantTypes(method).sections

        assertEquals(2, sections.size)
        assertTrue(sections.all { section -> section.kind == SectionKind.RELEVANT_TYPES })
        val text = sections.joinToString("\n") { section -> section.text }
        assertTrue(text.contains("class Order"))
        assertTrue(text.contains("private final String id;"))
        assertTrue(text.contains("String id() { /* body omitted */ }"))
        assertTrue(text.contains("class Receipt"))
        assertFalse(text.contains("class String"))
        assertFalse(text.contains("interface List"))
    }

    fun testExtractCallersIsSafeInDumbMode() {
        val file = myFixture.configureByText(
            "Sample.java",
            """
                class Sample {
                    int caller() {
                        return target();
                    }

                    int target() {
                        return 42;
                    }
                }
            """.trimIndent(),
        )
        val target = PsiTreeUtil.findChildrenOfType(file, PsiMethod::class.java)
            .single { it.name == "target" }

        DumbModeTestUtils.runInDumbModeSynchronously(project) {
            val result = JavaBackend().extractCallers(target)
            assertTrue(result.sections.isEmpty())
            assertTrue(
                result.omitted.any { entry ->
                    entry.kind == SectionKind.CALLERS && entry.reason.startsWith("dumb mode")
                },
            )
        }
    }
}
