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
    fun `priority follows section kind`() {
        val section = Section(SectionKind.CALLERS, text = "body", tokens = 1)

        assertEquals(SectionKind.CALLERS.priority, section.priority)
    }
}
