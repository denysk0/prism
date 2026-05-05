package com.prism.backend

data class Section(
    val kind: SectionKind,
    val text: String,
    val tokens: Int,
) {
    val priority: Int
        get() = kind.priority
}
