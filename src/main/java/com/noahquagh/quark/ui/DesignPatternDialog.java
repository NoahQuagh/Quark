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
 * Boîte de dialogue modale native IntelliJ permettant de choisir un patron de conception,
 * de saisir le nom de base des classes à générer et d'activer le mode pédagogique.
 *
 * @author noahq
 * @version 1.2
 */
public class DesignPatternDialog extends DialogWrapper {

    private final Project project;
    private final JComboBox<DesignPatternType> patternBox;
    private final JTextField nameField;
    private final JCheckBox pedagogicalCheckBox;

    /**
     * Initialise la fenêtre modale, ses composants Swing et la validation en temps réel sur le champ de nommage.
     *
     * @param project projet IntelliJ actif
     */
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

    /**
     * Construit et retourne le panneau central du formulaire aligné via {@link FormBuilder}.
     *
     * @return composant Swing principal de la boîte de dialogue
     */
    @Override
    public @Nullable JComponent createCenterPanel() {
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Patron :",patternBox)
                .addLabeledComponent("Nom de base :",nameField)
                .addComponent(pedagogicalCheckBox)
                .getPanel();
    }

    /**
     * Définit le champ texte du nom de base comme composant recevant automatiquement le focus à l'ouverture.
     *
     * @return le champ {@code nameField}
     */
    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return nameField;
    }

    /**
     * Valide en temps réel que le nom saisi est non vide, constitue un identifiant Java valide
     * et respecte la convention PascalCase (première lettre majuscule).
     *
     * @return une {@link ValidationInfo} décrivant l'erreur détectée, ou {@code null} si la saisie est valide
     */
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

    /**
     * Retourne le patron de conception sélectionné dans la liste déroulante.
     *
     * @return instance de {@link DesignPatternType} choisie
     */
    public DesignPatternType getSelectedPattern() {
        return (DesignPatternType) patternBox.getSelectedItem();
    }

    /**
     * Retourne le nom de base nettoyé saisi par l'utilisateur.
     *
     * @return nom de base sans espaces superflus
     */
    public String getBaseName() {
        return nameField.getText().trim();
    }

    /**
     * Indique si l'option d'ajout de commentaires Javadoc pédagogiques est cochée.
     *
     * @return {@code true} si le mode pédagogique est activé, {@code false} sinon
     */
    public boolean isPedagogical() {
        return pedagogicalCheckBox.isSelected();
    }
}
