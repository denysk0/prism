package com.prism.core

import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding
import com.knuddels.jtokkit.api.EncodingType

class JtokkitEstimator(
    private val encoding: Encoding = DEFAULT_ENCODING,
) : TokenEstimator {
    override fun estimate(text: String): Int = encoding.countTokens(text)

    private companion object {
        val DEFAULT_ENCODING: Encoding =
            Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE)
    }
}
