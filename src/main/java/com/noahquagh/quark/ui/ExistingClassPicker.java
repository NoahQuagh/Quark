package com.noahquagh.quark.ui;

import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Composant Swing permettant de sélectionner une classe ou interface Java existante
 * dans le projet via le sélecteur natif IntelliJ.
 *
 * @author noahq
 * @version 1.0
 */
public class ExistingClassPicker extends TextFieldWithBrowseButton {

    private PsiClass selectedClass;

    public ExistingClassPicker(@Nullable Project project, String dialogTitle, @Nullable Consumer<PsiClass> onClassSelected) {
        getTextField().setEditable(false);
        getTextField().setText("(Générer une nouvelle classe par défaut)");

        addActionListener(e -> {
            if (project == null) return;
            TreeClassChooser chooser = TreeClassChooserFactory.getInstance(project)
                    .createWithInnerClassesScopeChooser(
                            dialogTitle,
                            GlobalSearchScope.projectScope(project),
                            psiClass -> !psiClass.isEnum() && !psiClass.isAnnotationType(),
                            selectedClass
                    );
            chooser.showDialog();
            PsiClass chosen = chooser.getSelected();
            if (chosen != null) {
                this.selectedClass = chosen;
                getTextField().setText(chosen.getQualifiedName());
                if (onClassSelected != null) {
                    onClassSelected.accept(chosen);
                }
            }
        });
    }

    public @Nullable PsiClass getSelectedClass() {
        return selectedClass;
    }

    public void clearSelection() {
        this.selectedClass = null;
        getTextField().setText("(Générer une nouvelle classe par défaut)");
    }
}