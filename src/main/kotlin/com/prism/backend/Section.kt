package com.prism.backend

data class Section(
    val kind: SectionKind,
    val priority: Int,
    val text: String,
    val tokens: Int,
) {
    constructor(kind: SectionKind, text: String, tokens: Int) : this(kind, kind.priority, text, tokens)
}
