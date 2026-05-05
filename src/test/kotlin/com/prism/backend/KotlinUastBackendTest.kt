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
}
