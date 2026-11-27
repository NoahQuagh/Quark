package com.noahquagh.quark.jcb.psi;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.codeStyle.CodeStyleManager;

import java.util.Map;

/**
 * Utilitaire responsable de l'écriture sécurisée des fichiers Java générés dans l'arbre PSI d'IntelliJ IDEA.
 *
 * @author noahq
 * @version 1.0
 */
public class PsiFileWriter {

    /**
     * Crée et formate physiquement les fichiers Java dans le répertoire cible au sein d'une {@link WriteCommandAction},
     * puis ouvre automatiquement le premier fichier créé dans l'éditeur.
     *
     * @param project       projet IntelliJ actif
     * @param directory     répertoire cible dans lequel ajouter les fichiers
     * @param commandName   intitulé de l'action affiché dans l'historique d'annulation (Ctrl + Z)
     * @param filesToCreate table associant chaque nom de fichier à son contenu source Java
     */
    public static void writeFiles(Project project, PsiDirectory directory, String commandName, Map<String, String> filesToCreate) {
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

                FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(fileName);
                PsiFile psiFile = PsiFileFactory.getInstance(project)
                        .createFileFromText(fileName, fileType, code);
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
