package com.prism.core

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class CapsulePublicationState {
    @Volatile
    private var lastEvent: CapsulePublishedEvent? = null

    fun publish(event: CapsulePublishedEvent) {
        lastEvent = event
    }

    fun latest(): CapsulePublishedEvent? = lastEvent

    companion object {
        @JvmStatic
        fun getInstance(project: Project): CapsulePublicationState = project.service()
    }
}
