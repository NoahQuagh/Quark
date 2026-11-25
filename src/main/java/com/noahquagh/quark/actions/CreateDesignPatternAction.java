package com.noahquagh.quark.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.noahquagh.quark.jcb.PatternGenerator;
import com.noahquagh.quark.ui.DesignPatternDialog;
import org.jetbrains.annotations.NotNull;

/**
 * Action IntelliJ déclenchant la génération d'un patron de conception dans le package sélectionné.
 * Accessible via le menu contextuel {@code New} ou {@code Generate} ({@code Alt + Insert}).
 *
 * @author noahq
 * @version 1.3
 */
public class CreateDesignPatternAction extends AnAction{

    /**
     * Initialise l'action avec son libellé, sa description et son icône native IntelliJ.
     */
    public CreateDesignPatternAction() {
        super("Java Design Pattern", "Générer la structure Java d'un patron de conception", AllIcons.Nodes.Models);
    }

    /**
     * Ouvre la boîte de dialogue de configuration et lance la génération des fichiers du patron choisi.
     *
     * @param e événement déclencheur contenant le contexte du projet et du répertoire ciblé
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        PsiDirectory dir = null;
        PsiClass targetClientClass = null;

        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);

        if (file instanceof PsiJavaFile javaFile && javaFile.getClasses().length > 0) {
            targetClientClass = javaFile.getClasses()[0];
        }

        IdeView view = e.getData(LangDataKeys.IDE_VIEW);
        if (view != null) {
            dir = view.getOrChooseDirectory();
        } else if (file != null) {
            dir = file.getContainingDirectory();
        }

        String packageName = "";
        if (dir != null) {
            PsiPackage pkg = JavaDirectoryService.getInstance().getPackage(dir);
            if (pkg != null) packageName = pkg.getQualifiedName();
        }

        String targetClientClassName = (targetClientClass != null) ? targetClientClass.getName() : null;
        DesignPatternDialog dialog = new DesignPatternDialog(project,packageName,targetClientClassName);
        if (dialog.showAndGet()) {
            PatternGenerator.generate(project, dir, dialog.getConfig(),targetClientClass);
        }

    }

    /**
     * Active et rend visible l'action uniquement lorsqu'un projet est ouvert dans l'IDE.
     *
     * @param e événement de mise à jour de l'interface
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.getProject() != null);
    }

    /**
     * Spécifie que la mise à jour de l'état de l'action s'exécute sur un thread d'arrière-plan (BGT).
     *
     * @return {@link ActionUpdateThread#BGT}
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
