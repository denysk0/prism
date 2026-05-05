package com.prism.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.psi.PsiElement;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.prism.core.CapsuleBuilder;
import com.prism.core.CapsulePublicationState;
import com.prism.core.CapsulePublishedEvent;
import com.prism.core.CapsulePublishedListener;
import com.prism.core.CapsuleRequestContext;
import com.prism.core.CapsuleTree;
import com.prism.core.CapsuleTreeNode;
import com.prism.core.CapsulePublishedTopic;
import kotlinx.serialization.json.Json;
import kotlinx.serialization.json.JsonArray;
import kotlinx.serialization.json.JsonElement;
import kotlinx.serialization.json.JsonObject;
import kotlinx.serialization.json.JsonPrimitive;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.datatransfer.StringSelection;
import java.util.Locale;

public final class PrismToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        PreviewPanel panel = new PreviewPanel(project);

        project.getMessageBus().connect(toolWindow.getDisposable()).subscribe(
            CapsulePublishedTopic.INSTANCE.getTOPIC(),
            (CapsulePublishedListener) event -> ApplicationManager.getApplication().invokeLater(() -> {
                panel.setCapsule(event);
            })
        );

        CapsulePublishedEvent latest = CapsulePublicationState.getInstance(project).latest();
        if (latest != null) {
            panel.setCapsule(latest);
        }

        Content content = ContentFactory.getInstance().createContent(panel, null, false);
        toolWindow.getContentManager().addContent(content);
    }

    private static final class PreviewPanel extends JPanel {
        private static final Dimension GAUGE_SIZE = new Dimension(220, 18);

        private final Project project;
        private final JTextArea preview = new JTextArea();
        private final JTree tree = new JTree(emptyTreeModel());
        private final JProgressBar budgetGauge = new JProgressBar();
        private final JBLabel budgetGaugeLabel = new JBLabel("No budget");
        private final JBLabel tokensLabel = new JBLabel("Tokens: -");
        private final JBLabel budgetLabel = new JBLabel("Budget: -");
        private final JBLabel naiveTokensLabel = new JBLabel("Naive: -");
        private final JBLabel savedPctLabel = new JBLabel("Saved: -");
        private final JButton copyJsonButton = new JButton("Copy JSON");
        private final JButton copyMarkdownButton = new JButton("Copy Markdown");
        private final JButton rerunButton = new JButton("Refresh");
        private final JButton budgetButton = new JButton("Budget...");
        private String lastCapsule;
        private CapsuleRequestContext lastRequestContext;
        private boolean rebuilding;

        private PreviewPanel(Project project) {
            super(new BorderLayout());
            this.project = project;
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            preview.setEditable(false);
            preview.setLineWrap(false);
            preview.setText("No capsule published yet.");
            preview.setFont(new Font(Font.MONOSPACED, Font.PLAIN, preview.getFont().getSize()));
            preview.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

            tree.setRootVisible(true);
            tree.setShowsRootHandles(true);
            tree.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent event) {
                    if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)) {
                        navigateSelectedNode();
                    }
                }
            });

            budgetGauge.setMinimum(0);
            budgetGauge.setMaximum(100);
            budgetGauge.setValue(0);
            budgetGauge.setStringPainted(false);
            budgetGauge.setPreferredSize(GAUGE_SIZE);
            budgetGauge.setMinimumSize(GAUGE_SIZE);

            JPanel stats = new JPanel(new GridLayout(1, 4, 8, 0));
            stats.add(tokensLabel);
            stats.add(budgetLabel);
            stats.add(naiveTokensLabel);
            stats.add(savedPctLabel);

            JPanel header = new JPanel();
            header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
            header.add(budgetGauge);
            header.add(Box.createVerticalStrut(2));
            header.add(budgetGaugeLabel);
            header.add(Box.createVerticalStrut(6));
            header.add(stats);
            header.add(Box.createVerticalStrut(6));
            header.add(createActions());
            header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

            add(header, BorderLayout.NORTH);
            JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JBScrollPane(tree),
                new JBScrollPane(preview)
            );
            splitPane.setResizeWeight(0.22);
            splitPane.setContinuousLayout(true);
            splitPane.setBorder(BorderFactory.createEmptyBorder());
            add(splitPane, BorderLayout.CENTER);
        }

        private void setCapsule(CapsulePublishedEvent event) {
            String capsule = event.getCapsuleJson();
            lastCapsule = capsule;
            lastRequestContext = event.getRequestContext();
            preview.setText(capsule);
            preview.setCaretPosition(0);
            updateStats(CapsuleStats.from(capsule));
            updateTree(event.getTree());
            updateActions();
        }

        private void updateTree(CapsuleTree capsuleTree) {
            tree.setModel(capsuleTree == null ? emptyTreeModel() : new DefaultTreeModel(toSwingNode(capsuleTree.getRoot())));
            for (int row = 0; row < tree.getRowCount(); row++) {
                tree.expandRow(row);
            }
        }

        private static DefaultMutableTreeNode toSwingNode(CapsuleTreeNode node) {
            DefaultMutableTreeNode swingNode = new DefaultMutableTreeNode(new TreeUserObject(node));
            for (CapsuleTreeNode child : node.getChildren()) {
                swingNode.add(toSwingNode(child));
            }
            return swingNode;
        }

        private static DefaultTreeModel emptyTreeModel() {
            return new DefaultTreeModel(new DefaultMutableTreeNode("No navigation"));
        }

        private void navigateSelectedNode() {
            TreePath selection = tree.getSelectionPath();
            if (selection == null) {
                return;
            }
            Object selected = ((DefaultMutableTreeNode) selection.getLastPathComponent()).getUserObject();
            if (!(selected instanceof TreeUserObject treeObject)) {
                return;
            }
            CapsuleTreeNode node = treeObject.node;
            if (node.getPointer() == null) {
                return;
            }

            OpenFileDescriptor descriptor = ReadAction.compute(() -> {
                PsiElement element = node.getPointer().getElement();
                if (element == null || !element.isValid()) {
                    return null;
                }
                var virtualFile = element.getContainingFile() == null ? null : element.getContainingFile().getVirtualFile();
                if (virtualFile == null) {
                    return null;
                }
                return new OpenFileDescriptor(project, virtualFile, element.getTextOffset());
            });

            if (descriptor != null) {
                descriptor.navigate(true);
            }
        }

        private JPanel createActions() {
            copyJsonButton.setEnabled(false);
            copyMarkdownButton.setEnabled(false);
            rerunButton.setEnabled(false);
            budgetButton.setEnabled(false);
            copyJsonButton.addActionListener(event -> copyToClipboard(lastCapsule));
            copyMarkdownButton.addActionListener(event -> copyToClipboard(CapsuleMarkdown.fromJson(lastCapsule)));
            rerunButton.addActionListener(event -> rerunWithCurrentBudget());
            budgetButton.addActionListener(event -> promptAndRerunWithBudget());

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            actions.add(copyJsonButton);
            actions.add(Box.createHorizontalStrut(6));
            actions.add(copyMarkdownButton);
            actions.add(Box.createHorizontalStrut(12));
            actions.add(rerunButton);
            actions.add(Box.createHorizontalStrut(6));
            actions.add(budgetButton);
            return actions;
        }

        private void updateActions() {
            boolean hasCapsule = lastCapsule != null && !lastCapsule.isBlank();
            copyJsonButton.setEnabled(hasCapsule);
            copyMarkdownButton.setEnabled(hasCapsule);
            boolean canRebuild = lastRequestContext != null && !rebuilding;
            rerunButton.setEnabled(canRebuild);
            budgetButton.setEnabled(canRebuild);
        }

        private void rerunWithCurrentBudget() {
            CapsuleRequestContext context = lastRequestContext;
            if (context == null) {
                Messages.showInfoMessage(project, "This capsule does not include re-run context.", "Prism Re-run");
                updateActions();
                return;
            }
            rebuild(context, context.getBudget());
        }

        private void promptAndRerunWithBudget() {
            CapsuleRequestContext context = lastRequestContext;
            if (context == null) {
                Messages.showInfoMessage(project, "This capsule does not include re-run context.", "Prism Re-run");
                updateActions();
                return;
            }

            String value = Messages.showInputDialog(
                project,
                "Token budget:",
                "Re-run Capsule",
                null,
                Integer.toString(context.getBudget()),
                null
            );
            if (value == null) {
                return;
            }

            int budget;
            try {
                budget = Integer.parseInt(value.trim());
            } catch (NumberFormatException ignored) {
                Messages.showErrorDialog(project, "Budget must be a positive integer.", "Invalid Budget");
                return;
            }
            if (budget <= 0) {
                Messages.showErrorDialog(project, "Budget must be a positive integer.", "Invalid Budget");
                return;
            }

            rebuild(context, budget);
        }

        private void rebuild(CapsuleRequestContext context, int budget) {
            if (context.getProject().isDisposed()) {
                Messages.showErrorDialog(project, "The original project is no longer available.", "Prism Re-run Failed");
                return;
            }

            rebuilding = true;
            updateActions();
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Re-run Prism capsule", false) {
                @Override
                public void run(ProgressIndicator indicator) {
                    try {
                        CapsuleBuilder.productionEstimatorBuilder().buildBlocking(
                            context.getProject(),
                            context.getFilePath(),
                            context.getLine(),
                            budget
                        );
                    } catch (Throwable throwable) {
                        ApplicationManager.getApplication().invokeLater(() ->
                            Messages.showErrorDialog(
                                project,
                                failureMessage(throwable),
                                "Prism Re-run Failed"
                            )
                        );
                    } finally {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            rebuilding = false;
                            updateActions();
                        });
                    }
                }
            });
        }

        private static String failureMessage(Throwable throwable) {
            String message = throwable.getMessage();
            if (message == null || message.isBlank()) {
                return "Capsule rebuild failed: " + throwable.getClass().getSimpleName();
            }
            return "Capsule rebuild failed: " + message;
        }

        private static void copyToClipboard(String value) {
            if (value == null || value.isBlank()) {
                return;
            }
            CopyPasteManager.getInstance().setContents(new StringSelection(value));
        }

        private void updateStats(CapsuleStats stats) {
            tokensLabel.setText("Tokens: " + displayInt(stats.tokens));
            budgetLabel.setText("Budget: " + displayInt(stats.budget));
            naiveTokensLabel.setText("Naive: " + displayInt(stats.naiveTokens));
            savedPctLabel.setText("Saved: " + displayPct(stats.savedPct));

            if (stats.tokens < 0 || stats.budget <= 0) {
                budgetGauge.setMaximum(100);
                budgetGauge.setValue(0);
                budgetGaugeLabel.setText("Budget unavailable");
                budgetGauge.setForeground(JBColor.GRAY);
                return;
            }

            int cappedTokens = Math.min(stats.tokens, stats.budget);
            int pct = (int) Math.round((stats.tokens * 100.0) / stats.budget);
            budgetGauge.setMaximum(stats.budget);
            budgetGauge.setValue(cappedTokens);
            budgetGaugeLabel.setText(stats.tokens + " / " + stats.budget + " tokens (" + pct + "%)");
            budgetGauge.setForeground(stats.tokens > stats.budget ? JBColor.RED : JBColor.BLUE);
        }

        private static String displayInt(int value) {
            return value < 0 ? "-" : Integer.toString(value);
        }

        private static String displayPct(double value) {
            return value < 0 ? "-" : String.format(Locale.US, "%.1f%%", value);
        }
    }

    private static final class TreeUserObject {
        private final CapsuleTreeNode node;

        private TreeUserObject(CapsuleTreeNode node) {
            this.node = node;
        }

        @Override
        public String toString() {
            return node.getLabel();
        }
    }

    private static final class CapsuleMarkdown {
        private CapsuleMarkdown() {
        }

        private static String fromJson(String capsule) {
            if (capsule == null || capsule.isBlank()) {
                return "";
            }

            try {
                JsonElement root = Json.Default.parseToJsonElement(capsule);
                if (!(root instanceof JsonObject rootObject)) {
                    return fallback(capsule);
                }
                JsonElement sectionsElement = rootObject.get("sections");
                if (!(sectionsElement instanceof JsonArray sections)) {
                    return fallback(capsule);
                }

                StringBuilder markdown = new StringBuilder();
                for (JsonElement sectionElement : sections) {
                    if (!(sectionElement instanceof JsonObject section)) {
                        continue;
                    }
                    String kind = primitiveContent(section.get("kind"));
                    String text = primitiveContent(section.get("text"));
                    if (text == null) {
                        continue;
                    }
                    if (!markdown.isEmpty()) {
                        markdown.append("\n\n");
                    }
                    String heading = kind == null || kind.isBlank() ? "SECTION" : kind;
                    String fence = codeFenceFor(text);
                    markdown.append("## ").append(heading).append('\n')
                        .append(fence).append('\n')
                        .append(text);
                    if (!text.endsWith("\n")) {
                        markdown.append('\n');
                    }
                    markdown.append(fence).append('\n');
                }

                return markdown.isEmpty() ? fallback(capsule) : markdown.toString();
            } catch (RuntimeException ignored) {
                return fallback(capsule);
            }
        }

        private static String fallback(String capsule) {
            String fence = codeFenceFor(capsule);
            return fence + "\n" + capsule + (capsule.endsWith("\n") ? "" : "\n") + fence + "\n";
        }

        private static String primitiveContent(JsonElement element) {
            if (!(element instanceof JsonPrimitive primitive)) {
                return null;
            }
            return primitive.getContent();
        }

        private static String codeFenceFor(String text) {
            int longestRun = 0;
            int currentRun = 0;
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == '`') {
                    currentRun++;
                    longestRun = Math.max(longestRun, currentRun);
                } else {
                    currentRun = 0;
                }
            }
            return "`".repeat(Math.max(3, longestRun + 1));
        }
    }

    private static final class CapsuleStats {
        private static final CapsuleStats UNAVAILABLE = new CapsuleStats(-1, -1, -1, -1.0);

        private final int tokens;
        private final int budget;
        private final int naiveTokens;
        private final double savedPct;

        private CapsuleStats(int tokens, int budget, int naiveTokens, double savedPct) {
            this.tokens = tokens;
            this.budget = budget;
            this.naiveTokens = naiveTokens;
            this.savedPct = savedPct;
        }

        private static CapsuleStats from(String capsule) {
            try {
                JsonElement root = Json.Default.parseToJsonElement(capsule);
                if (!(root instanceof JsonObject rootObject)) {
                    return UNAVAILABLE;
                }
                JsonElement statsElement = rootObject.get("stats");
                if (!(statsElement instanceof JsonObject stats)) {
                    return UNAVAILABLE;
                }
                return new CapsuleStats(
                    intValue(stats.get("tokens")),
                    intValue(stats.get("budget")),
                    intValue(stats.get("naiveTokens")),
                    doubleValue(stats.get("savedPct"))
                );
            } catch (RuntimeException ignored) {
                return UNAVAILABLE;
            }
        }

        private static int intValue(JsonElement element) {
            if (!(element instanceof JsonPrimitive primitive)) {
                return -1;
            }
            try {
                return Integer.parseInt(primitive.getContent());
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }

        private static double doubleValue(JsonElement element) {
            if (!(element instanceof JsonPrimitive primitive)) {
                return -1.0;
            }
            try {
                return Double.parseDouble(primitive.getContent());
            } catch (NumberFormatException ignored) {
                return -1.0;
            }
        }
    }
}
