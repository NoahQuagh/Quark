package com.noahquagh.quark.generator;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.codeStyle.CodeStyleManager;

import java.util.Map;

public class PsiFileWriter {
    public static void writeFiles(Project project, PsiDirectory directory, String commandName, Map<String,String> filesToCreate){
        WriteCommandAction.runWriteCommandAction(project, commandName, null, () -> {
            PsiFile firstCreated = null;

            for (Map.Entry<String, String> entry : filesToCreate.entrySet()) {
                String fileName = entry.getKey();
                String code = entry.getValue();

                if (directory.findFile(fileName) != null) {
                    Messages.showWarningDialog(project,
                            "Le fichier " + fileName + " existe déjà et a été ignoré.",
                            "Fichier existant");
                    continue;
                }

                PsiFile psiFile = PsiFileFactory.getInstance(project)
                        .createFileFromText(fileName, JavaFileType.INSTANCE, code);
                CodeStyleManager.getInstance(project).reformat(psiFile);

                PsiFile added = (PsiFile) directory.add(psiFile);
                if (firstCreated == null) {
                    firstCreated = added;
                }
            }

            if (firstCreated != null && firstCreated.getVirtualFile() != null) {
                FileEditorManager.getInstance(project).openFile(firstCreated.getVirtualFile(), true);
            }
        });
    }
}
