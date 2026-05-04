package com.prism.backend

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SectionTest {
    @Test
    fun `default constructor uses section kind priority`() {
        SectionKind.entries.forEach { kind ->
            val section = Section(kind, text = "body", tokens = 1)

            assertEquals(kind.priority, section.priority)
        }
    }

    @Test
    fun `primary constructor keeps explicit priority`() {
        val section = Section(SectionKind.TARGET, priority = 7, text = "body", tokens = 1)

        assertEquals(7, section.priority)
    }
}
