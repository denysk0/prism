package com.prism.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.prism.core.CapsulePublishedListener
import com.prism.core.CapsulePublishedTopic
import java.awt.BorderLayout
import javax.swing.JScrollPane
import javax.swing.JPanel
import javax.swing.JTextArea

class PrismToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val textArea = JTextArea().apply {
            isEditable = false
            lineWrap = false
            text = "Prism"
        }
        val panel = JPanel(BorderLayout())
        panel.add(JScrollPane(textArea), BorderLayout.CENTER)

        project.messageBus.connect(toolWindow.disposable).subscribe(
            CapsulePublishedTopic.TOPIC,
            CapsulePublishedListener { capsule ->
                ApplicationManager.getApplication().invokeLater {
                    textArea.text = capsule
                    textArea.caretPosition = 0
                }
            },
        )

        val content = ContentFactory.getInstance().createContent(panel, null, false)
        toolWindow.contentManager.addContent(content)
    }
}
