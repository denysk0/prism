package com.prism.core

import com.intellij.util.messages.Topic

fun interface CapsulePublishedListener {
    fun capsulePublished(capsule: String)
}

object CapsulePublishedTopic {
    val TOPIC: Topic<CapsulePublishedListener> =
        Topic.create("Prism Capsule Published", CapsulePublishedListener::class.java)
}
