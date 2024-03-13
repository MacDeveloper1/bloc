package com.bloc.intellij_generator_plugin.action

import com.bloc.intellij_generator_plugin.generator.BlocGeneratorFactory
import com.bloc.intellij_generator_plugin.generator.BlocGenerator
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.project.Project
import com.intellij.psi.*

class GenerateBlocAction : AnAction(), GenerateBlocDialog.Listener {

    private lateinit var dataContext: DataContext

    override fun actionPerformed(e: AnActionEvent) {
        val dialog = GenerateBlocDialog(this)
        dialog.show()
    }

    override fun onGenerateBlocClicked(
        blocName: String?,
        blocTemplateType: BlocTemplateType,
    ) {
        blocName?.let { name ->
            val generators = BlocGeneratorFactory.getBlocGenerators(name, blocTemplateType)
            generate(generators)
        }
    }

    override fun update(e: AnActionEvent) {
        e.dataContext.let {
            this.dataContext = it
            val presentation = e.presentation
            presentation.isEnabled = true
        }
    }

    private fun generate(mainSourceGenerators: List<BlocGenerator>) {
        val project = CommonDataKeys.PROJECT.getData(dataContext)
        val view = LangDataKeys.IDE_VIEW.getData(dataContext)
        val directory = view?.orChooseDirectory
        ApplicationManager.getApplication().runWriteAction {
            CommandProcessor.getInstance().executeCommand(
                project, {
                    mainSourceGenerators.forEach { createSourceFile(project!!, it, directory!!) }
                }, "Generate a new Bloc", null
            )
        }
    }

    private fun createSourceFile(project: Project, generator: BlocGenerator, directory: PsiDirectory) {
        val fileName = generator.fileName()
        val existingPsiFile = directory.findFile(fileName)
        if (existingPsiFile != null) {
            val document = PsiDocumentManager.getInstance(project).getDocument(existingPsiFile)
            document?.insertString(document.textLength, "\n" + generator.generate())
            return
        }
        val psiFile = PsiFileFactory.getInstance(project)
            .createFileFromText(fileName, JavaLanguage.INSTANCE, generator.generate())
        directory.add(psiFile)
    }
}
