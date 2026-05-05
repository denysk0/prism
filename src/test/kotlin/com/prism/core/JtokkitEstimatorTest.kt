package com.prism.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JtokkitEstimatorTest {
    @Test
    fun `estimates non-zero tokens for non-empty text`() {
        val estimator = JtokkitEstimator()

        assertTrue(estimator.estimate("Hello world") > 0)
    }

    @Test
    fun `estimates zero tokens for empty text`() {
        val estimator = JtokkitEstimator()

        assertEquals(0, estimator.estimate(""))
    }

    @Test
    fun `differs from chars-by-four for code-like input`() {
        val text = "fun answer(): Int = 42"
        val jtokkit = JtokkitEstimator().estimate(text)
        val charsByFour = CharsBy4Estimator.estimate(text)

        assertTrue(jtokkit > 0)
        assertTrue(jtokkit != charsByFour)
    }
}
