package com.github.noahquagh.quark.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;

/**
 * @author noahq
 * @version 1.2
 */
public class CreateDesignPatternAction extends AnAction{

    public CreateDesignPatternAction() {
        super("Design Pattern", "Générer la structure d'un patron de conception", AllIcons.Nodes.ModelClass);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        PsiDirectory dir = null;
        IdeView view = e.getData(LangDataKeys.IDE_VIEW);
        if (view != null) {
            dir = view.getOrChooseDirectory();
        } else {
            PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
            if (file != null) dir = file.getContainingDirectory();
        }

        String packageName = "(inconnu)";
        if (dir != null) {
            PsiPackage pkg = JavaDirectoryService.getInstance().getPackage(dir);
            if (pkg != null) packageName = pkg.getQualifiedName();
        }

        Messages.showInfoMessage(project, "Package détecté : " + packageName, "Design Pattern");
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.getProject() != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
