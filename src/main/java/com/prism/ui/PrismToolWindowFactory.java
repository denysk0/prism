package com.prism.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.prism.core.CapsulePublishedListener;
import com.prism.core.CapsulePublishedTopic;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;

public final class PrismToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(false);
        textArea.setText("Prism");

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);

        project.getMessageBus().connect(toolWindow.getDisposable()).subscribe(
            CapsulePublishedTopic.INSTANCE.getTOPIC(),
            (CapsulePublishedListener) capsule -> ApplicationManager.getApplication().invokeLater(() -> {
                textArea.setText(capsule);
                textArea.setCaretPosition(0);
            })
        );

        Content content = ContentFactory.getInstance().createContent(panel, null, false);
        toolWindow.getContentManager().addContent(content);
    }
}
