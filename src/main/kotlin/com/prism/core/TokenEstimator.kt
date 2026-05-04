package com.prism.core

interface TokenEstimator {
    fun estimate(text: String): Int
}

object CharsBy4Estimator : TokenEstimator {
    override fun estimate(text: String): Int = text.length / 4
}
