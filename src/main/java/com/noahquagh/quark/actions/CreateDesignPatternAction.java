package com.noahquagh.quark.actions;

import com.noahquagh.quark.generator.PatternGenerator;
import com.noahquagh.quark.model.DesignPatternType;
import com.noahquagh.quark.model.PatternGenerationConfig;
import com.noahquagh.quark.ui.DesignPatternDialog;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

        DesignPatternDialog dialog = new DesignPatternDialog(project);
        if (dialog.showAndGet()) {
            PatternGenerationConfig config = dialog.getConfig();

            DesignPatternType pattern = config.pattern();
            String baseName = config.baseName();
            boolean createSubPackage = config.createSubPackage();
            boolean pedagogical = config.pedagogical();
            Set<String> enabledOptionals = config.enabledOptionalRoleIds();
            Map<String, Integer> instanceCounts = config.roleInstanceCounts();
            List<String> variants = config.variants().isEmpty() ? List.of("A", "B") : config.variants();

            PatternGenerator.generate(project, dir, config);
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
