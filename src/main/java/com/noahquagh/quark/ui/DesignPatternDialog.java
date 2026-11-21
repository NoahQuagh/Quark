package com.noahquagh.quark.ui;

import com.noahquagh.quark.model.DesignPatternType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.psi.PsiNameHelper;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author noahq
 * @version 1.2
 */
public class DesignPatternDialog extends DialogWrapper {

    private final Project project;
    private final JComboBox<DesignPatternType> patternBox;
    private final JTextField nameField;
    private final JCheckBox pedagogicalCheckBox;

    public DesignPatternDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        this.patternBox = new ComboBox<>(DesignPatternType.values());
        this.nameField = new JTextField();
        this.pedagogicalCheckBox = new JCheckBox("Mode pédagogique");
        nameField.getDocument().addDocumentListener(new com.intellij.ui.DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull javax.swing.event.DocumentEvent e) {
                initValidation();
            }
        });
        setTitle("Generate Pattern Design");
        init();
    }

    @Override
    public @Nullable JComponent createCenterPanel() {
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Patron :",patternBox)
                .addLabeledComponent("Nom de base :",nameField)
                .addComponent(pedagogicalCheckBox)
                .getPanel();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return nameField;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            return new ValidationInfo("Veuillez saisir un nom de base (ex: Payment)", nameField);
        }
        if (!PsiNameHelper.getInstance(project).isIdentifier(name)) {
            return new ValidationInfo("Le nom doit être un identifiant Java valide", nameField);
        }
        if (!Character.isUpperCase(name.charAt(0))) {
            return new ValidationInfo("Une classe Java doit commencer par une majuscule (PascalCase)", nameField);
        }
        return null;
    }

    public DesignPatternType getSelectedPattern() {
        return (DesignPatternType) patternBox.getSelectedItem();
    }

    public String getBaseName() {
        return nameField.getText().trim();
    }

    public boolean isPedagogical() {
        return pedagogicalCheckBox.isSelected();
    }
}
