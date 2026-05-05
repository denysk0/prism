package com.prism.core

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.util.messages.Topic
import com.prism.backend.SectionKind

data class CapsulePublishedEvent(
    val capsuleJson: String,
    val tree: CapsuleTree? = null,
    val requestContext: CapsuleRequestContext? = null,
)

data class CapsuleRequestContext(
    val project: Project,
    val filePath: String,
    val line: Int,
    val budget: Int,
)

data class CapsuleTree(
    val root: CapsuleTreeNode,
)

data class CapsuleTreeNode(
    val label: String,
    val pointer: SmartPsiElementPointer<PsiElement>?,
    val kind: SectionKind? = null,
    val children: List<CapsuleTreeNode> = emptyList(),
)

data class CapsuleNavigationTarget(
    val label: String,
    val pointer: SmartPsiElementPointer<PsiElement>,
)

fun interface CapsulePublishedListener {
    fun capsulePublished(event: CapsulePublishedEvent)
}

object CapsulePublishedTopic {
    val TOPIC: Topic<CapsulePublishedListener> =
        Topic.create("Prism Capsule Published", CapsulePublishedListener::class.java)
}
