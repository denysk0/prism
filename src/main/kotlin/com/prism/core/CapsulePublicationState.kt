package com.prism.core

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Project-scoped cache of the most recently published capsule event.
 *
 * The stored [CapsulePublishedEvent] retains references to [com.intellij.psi.SmartPsiElementPointer]s
 * (via [CapsuleTree]) and to the originating [Project] (via [CapsuleRequestContext]). To avoid
 * leaking those references past project close, this service implements [Disposable] and clears
 * [lastEvent] when the platform disposes the service on project teardown.
 */
@Service(Service.Level.PROJECT)
class CapsulePublicationState : Disposable {
    @Volatile
    private var lastEvent: CapsulePublishedEvent? = null

    fun publish(event: CapsulePublishedEvent) {
        lastEvent = event
    }

    fun latest(): CapsulePublishedEvent? = lastEvent

    fun clear() {
        lastEvent = null
    }

    override fun dispose() {
        clear()
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): CapsulePublicationState = project.service()
    }
}
