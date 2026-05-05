package com.prism.backend

import com.prism.core.CapsuleNavigationTarget

data class Section(
    val kind: SectionKind,
    val text: String,
    val tokens: Int,
    val navigation: CapsuleNavigationTarget? = null,
    val reduced: Section? = null,
) {
    val priority: Int
        get() = kind.priority
}
