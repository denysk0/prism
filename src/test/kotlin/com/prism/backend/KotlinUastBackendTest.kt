package com.prism.backend

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

class KotlinUastBackendTest : LightJavaCodeInsightFixtureTestCase() {
    fun testExtractTargetReturnsFunctionText() {
        val source = """
            class Sample {
                fun helper(): Int {
                    return 1
                }

                fun target(input: Int): Int {
                    val doubled = input * 2
                    return doubled
                }
            }
        """.trimIndent()
        val file = myFixture.configureByText("Sample.kt", source)
        val element = file.findElementAt(source.indexOf("val doubled"))!!

        val section = KotlinUastBackend().extractTarget(element)

        assertNotNull(section)
        assertEquals(SectionKind.TARGET, section!!.kind)
        assertTrue(section.text.contains("fun target(input: Int): Int"))
        assertTrue(section.text.contains("return doubled"))
        assertFalse(section.text.contains("fun helper()"))
    }

    fun testExtractTargetReturnsClassText() {
        val source = """
            class Sample {
                fun answer(): Int = 42
            }
        """.trimIndent()
        val file = myFixture.configureByText("Sample.kt", source)
        val element = file.findElementAt(source.indexOf("class Sample"))!!

        val section = KotlinUastBackend().extractTarget(element)

        assertNotNull(section)
        assertEquals(SectionKind.TARGET, section!!.kind)
        assertTrue(section.text.contains("class Sample"))
        assertTrue(section.text.contains("fun answer()"))
    }

    fun testExtractOwningClassSkeletonOmitsFunctionBodies() {
        val source = """
            class Sample(
                private val seed: Int,
            ) {
                val visible: Int = seed + 1

                fun helper(input: Int): Int {
                    val adjusted = input + visible
                    return adjusted
                }

                fun target(input: Int): Int {
                    val doubled = input * 2
                    return doubled
                }
            }
        """.trimIndent()
        val file = myFixture.configureByText("Sample.kt", source)
        val element = file.findElementAt(source.indexOf("val doubled"))!!

        val section = KotlinUastBackend().extractOwningClassSkeleton(element)

        assertNotNull(section)
        assertEquals(SectionKind.OWNING_SKELETON, section!!.kind)
        assertTrue(section.text.contains("class Sample("))
        assertTrue(section.text.contains("private val seed: Int"))
        assertTrue(section.text.contains("val visible: Int = seed + 1"))
        assertTrue(section.text.contains("fun helper(input: Int): Int { /* body omitted */ }"))
        assertFalse(section.text.contains("val adjusted"))
        assertFalse(section.text.contains("fun target(input: Int): Int"))
        assertFalse(section.text.contains("return doubled"))
    }

    fun testExtractOwningClassSkeletonReturnsNullForClassTarget() {
        val source = """
            class Sample {
                fun answer(): Int = 42
            }
        """.trimIndent()
        val file = myFixture.configureByText("Sample.kt", source)
        val element = file.findElementAt(source.indexOf("class Sample"))!!

        assertNull(KotlinUastBackend().extractOwningClassSkeleton(element))
    }

    fun testExtractCallersReturnsResolvedKotlinCallers() {
        val source = """
            class Sample {
                fun first(): Int {
                    return target()
                }

                fun second(): Int {
                    return target()
                }

                fun duplicate(): Int {
                    val value = target()
                    return value + target()
                }

                fun target(): Int {
                    return 42
                }
            }
        """.trimIndent()
        val file = myFixture.configureByText("Sample.kt", source)
        val element = file.findElementAt(source.indexOf("return 42"))!!

        val result = KotlinUastBackend().extractCallers(element)
        val text = result.sections.joinToString("\n") { section -> section.text }

        assertEquals(3, result.sections.size)
        assertTrue(result.sections.all { section -> section.kind == SectionKind.CALLERS })
        assertTrue(text.contains("fun first(): Int"))
        assertTrue(text.contains("fun second(): Int"))
        assertTrue(text.contains("fun duplicate(): Int"))
        assertTrue(text.contains("// return target()"))
        assertTrue(result.omitted.isEmpty())
    }

    fun testExtractCalleesReturnsResolvedKotlinCallees() {
        val source = """
            class Sample {
                fun target(): Int {
                    val total = first()
                    return total + second() + first()
                }

                fun first(): Int {
                    return 1
                }

                fun second(): Int {
                    return 2
                }
            }
        """.trimIndent()
        val file = myFixture.configureByText("Sample.kt", source)
        val element = file.findElementAt(source.indexOf("val total"))!!

        val result = KotlinUastBackend().extractCallees(element)
        val text = result.sections.joinToString("\n") { section -> section.text }

        assertEquals(2, result.sections.size)
        assertTrue(result.sections.all { section -> section.kind == SectionKind.INTERNAL_CALLEES })
        assertTrue(text.contains("fun first(): Int"))
        assertTrue(text.contains("fun second(): Int"))
        assertTrue(text.contains("// return 1"))
        assertTrue(result.omitted.isEmpty())
    }
}
