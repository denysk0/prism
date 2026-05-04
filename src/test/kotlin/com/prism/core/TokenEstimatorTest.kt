package com.prism.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TokenEstimatorTest {
    private val estimator: TokenEstimator = CharsBy4Estimator

    @Test
    fun `estimates hello as one token`() {
        assertEquals(1, estimator.estimate("hello"))
    }

    @Test
    fun `estimates empty text as zero tokens`() {
        assertEquals(0, estimator.estimate(""))
    }

    @Test
    fun `uses integer division by four characters`() {
        assertEquals(1, estimator.estimate("abcd"))
        assertEquals(2, estimator.estimate("abcdefgh"))
        assertEquals(2, estimator.estimate("abcdefghi"))
    }
}
