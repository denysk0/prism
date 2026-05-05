package com.prism.backend

enum class SectionKind(val priority: Int) {
    TARGET(100),
    FIELDS_USED(90),
    OWNING_SKELETON(80),
    INTERNAL_CALLEES(70),
    CALLEES_PARTIAL(65),
    EXTERNAL_CALLEES(60),
    RELEVANT_TYPES(50),
    CALLERS(40),
}
