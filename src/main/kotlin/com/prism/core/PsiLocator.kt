package com.prism.core

import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import java.nio.file.Path

data class LocateResult(
    val element: PsiElement?,
    val psiFile: PsiFile,
)

object PsiLocator {
    fun locate(project: Project, filePath: String, line: Int): LocateResult? {
        if (filePath.isBlank() || line < 1) {
            return null
        }

        val virtualFile = LocalFileSystem.getInstance().findFileByNioFile(Path.of(filePath))
            ?: return null
        if (!virtualFile.isValid || !virtualFile.isInLocalFileSystem) {
            return null
        }

        val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
            ?: return null
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
            ?: return null

        val zeroBasedLine = line - 1
        if (zeroBasedLine !in 0 until document.lineCount) {
            return LocateResult(element = null, psiFile = psiFile)
        }

        val element = findFirstNonWhitespaceElementOnLine(psiFile, document, zeroBasedLine)
            ?: return LocateResult(element = null, psiFile = psiFile)

        val target = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java, false)
            ?: PsiTreeUtil.getParentOfType(element, PsiClass::class.java, false)
            ?: element
        return LocateResult(element = target, psiFile = psiFile)
    }

    private fun findFirstNonWhitespaceElementOnLine(
        psiFile: PsiFile,
        document: Document,
        zeroBasedLine: Int,
    ): PsiElement? {
        val lineStart = document.getLineStartOffset(zeroBasedLine)
        val lineEnd = document.getLineEndOffset(zeroBasedLine)
        for (offset in lineStart until lineEnd) {
            val element = psiFile.findElementAt(offset)
            if (element != null && element !is PsiWhiteSpace) {
                return element
            }
        }

        return psiFile.findElementAt(lineStart)
    }
}
