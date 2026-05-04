package com.prism.core

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil

object PsiLocator {
    fun locate(project: Project, filePath: String, line: Int): PsiElement? {
        if (filePath.isBlank() || line < 1) {
            return null
        }

        return ApplicationManager.getApplication().runReadAction(
            Computable {
                val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath)
                    ?: return@Computable null
                if (!virtualFile.isValid) {
                    return@Computable null
                }

                val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                    ?: return@Computable null
                val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
                    ?: return@Computable null

                val zeroBasedLine = line - 1
                if (zeroBasedLine !in 0 until document.lineCount) {
                    return@Computable null
                }

                val element = psiFile.findElementAt(document.getLineStartOffset(zeroBasedLine))
                    ?: return@Computable null

                PsiTreeUtil.getParentOfType(element, PsiMethod::class.java, false)
                    ?: PsiTreeUtil.getParentOfType(element, PsiClass::class.java, false)
            },
        )
    }
}
