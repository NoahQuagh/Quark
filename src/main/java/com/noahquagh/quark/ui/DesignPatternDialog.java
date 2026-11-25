package com.noahquagh.quark.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.psi.PsiNameHelper;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.noahquagh.quark.model.ClassKind;
import com.noahquagh.quark.model.DesignPatternType;
import com.noahquagh.quark.model.PatternGenerationConfig;
import com.noahquagh.quark.model.PatternRole;
import com.noahquagh.quark.ui.config.PatternConfigPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Boîte de dialogue modale native IntelliJ permettant de configurer dynamiquement la génération
 * d'un patron de conception (choix du pattern, nom de base, sous-package, rôles optionnels,
 * multiplicité des instances) avec prévisualisation en temps réel des fichiers créés.
 *
 * @author noahq
 * @version 1.4
 */
public class DesignPatternDialog extends DialogWrapper {

    private final Project project;
    private final String targetPackageName;

    private final ComboBox<DesignPatternType> patternBox = new ComboBox<>(DesignPatternType.values());
    private final JBTextField nameField = new JBTextField("MyPattern");
    private final JBCheckBox subPackageCheckBox = new JBCheckBox("Créer un sous-package", true);
    private final JBCheckBox pedagogicalCheckBox = new JBCheckBox("Mode pédagogique (Javadoc & TODO)", true);
    private final JBCheckBox includeClientCheckBox = new JBCheckBox("Inclure une classe Client", true);

    private final JBLabel descriptionLabel = new JBLabel();
    private final JPanel rolesPanel = new JPanel();
    private final JPanel specificConfigContainer = new JPanel(new BorderLayout());

    private final DefaultListModel<PreviewItem> previewListModel = new DefaultListModel<>();
    private final JBList<PreviewItem> previewList = new JBList<>(previewListModel);

    private final Map<String, JBCheckBox> optionalRoleCheckBoxes = new LinkedHashMap<>();
    private final Map<String, JBIntSpinner> manyRoleSpinners = new LinkedHashMap<>();

    private PatternConfigPanel currentConfigPanel;

    private final String activeClassName;
    private JBCheckBox injectClientCheckBox;

    /**
     * Initialise la boîte de dialogue sans package cible prérempli.
     *
     * @param project projet IntelliJ actif
     */
    public DesignPatternDialog(@Nullable Project project) {
        this(project, "");
    }

    /**
     * Initialise la boîte de dialogue avec le nom du package cible sélectionné dans l'arborescence,
     * configure les écouteurs d'événements temps réel et affiche l'aperçu initial.
     *
     * @param project           projet IntelliJ actif
     * @param targetPackageName nom qualifié du package cible
     */
    public DesignPatternDialog(@Nullable Project project, @Nullable String targetPackageName) {
        this(project,targetPackageName,null);
    }

    /**
     * Initialise la boîte de dialogue avec le nom du package cible sélectionné dans l'arborescence,
     * configure les écouteurs d'événements temps réel et affiche l'aperçu initial.
     *
     * @param project           projet IntelliJ actif
     * @param targetPackageName nom qualifié du package cible
     * @param targetClientClassName nom de la classe client cible
     */
    public DesignPatternDialog(@Nullable Project project, @Nullable String targetPackageName,@Nullable String targetClientClassName) {
        super(project);

        this.activeClassName = targetClientClassName;
        this.injectClientCheckBox = new JBCheckBox(
                activeClassName != null ? "Injecter dans " + activeClassName + ".java (au lieu de créer un fichier Client)" : "",
                activeClassName != null
        );
        this.injectClientCheckBox.setVisible(false);
        this.injectClientCheckBox.addActionListener(e -> {
            updateClientRoleCheckboxState();
            refreshPreview();
        });

        includeClientCheckBox.addActionListener(e -> refreshPreview());

        this.project = project;
        this.targetPackageName = targetPackageName != null ? targetPackageName : "";

        rolesPanel.setLayout(new BoxLayout(rolesPanel, BoxLayout.Y_AXIS));
        configurePreviewList();

        patternBox.addActionListener(e -> onPatternChanged());
        subPackageCheckBox.addActionListener(e -> refreshPreview());
        nameField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                initValidation();
                refreshPreview();
            }
        });

        setTitle("Quark : Générer un Design Pattern en Java");
        onPatternChanged();
        init();
    }

    /**
     * Configure l'affichage de la liste d'aperçu avec les icônes natives IntelliJ
     * correspondant au type de chaque élément généré (interface, classe abstraite ou classe concrète).
     */
    private void configurePreviewList() {
        previewList.setVisibleRowCount(6);
        previewList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PreviewItem item) {
                    label.setText(item.displayPath());
                    label.setIcon(switch (item.kind()) {
                        case INTERFACE -> AllIcons.Nodes.Interface;
                        case ABSTRACT_CLASS -> AllIcons.Nodes.AbstractClass;
                        case CLASS -> AllIcons.Nodes.Class;
                    });
                }
                return label;
            }
        });
    }

    /**
     * Met à jour dynamiquement la description, les contrôles de rôles, le sous-panneau spécifique
     * et l'aperçu lorsque l'utilisateur sélectionne un autre patron dans la liste déroulante.
     */
    private void onPatternChanged() {
        DesignPatternType selected = getSelectedPattern();
        if (selected == null) return;

        descriptionLabel.setText("<html><body style='width: 400px; color: #888888;'>"
                + selected.getDescription() + "</body></html>");

        currentConfigPanel = selected.createConfigPanel();
        currentConfigPanel.setOnChangeListener(this::refreshPreview);
        specificConfigContainer.removeAll();
        specificConfigContainer.add(currentConfigPanel.getComponent(), BorderLayout.CENTER);

        boolean hasVariantsField = currentConfigPanel.getOptions().containsKey("variants");
        rebuildRolesControls(selected);

        Optional<PatternRole> clientRoleOpt = selected.findClientRole();
        boolean hasOptionalClient = clientRoleOpt.isPresent() && !clientRoleOpt.get().required() && "CLIENT".equals(clientRoleOpt.get().id());
        includeClientCheckBox.setVisible(hasOptionalClient);
        if (hasOptionalClient) {
            includeClientCheckBox.setText("Inclure " + clientRoleOpt.get().id() + " (" + clientRoleOpt.get().description() + ")");
        }

        boolean canInject = (activeClassName != null) && clientRoleOpt.isPresent();
        injectClientCheckBox.setVisible(canInject);
        updateClientRoleCheckboxState();

        refreshPreview();
        revalidateAndPack();
    }

    /**
     * Grise la case "Inclure CLIENT" si l'injection est cochée
     */
    private void updateClientRoleCheckboxState() {
        if (!includeClientCheckBox.isVisible()) return;
        boolean injecting = injectClientCheckBox.isVisible() && injectClientCheckBox.isSelected();
        includeClientCheckBox.setEnabled(!injecting);
        if (injecting) {
            includeClientCheckBox.setSelected(false);
        }
    }

    /**
     * Reconstruit dynamiquement les cases à cocher pour les rôles facultatifs
     * et les sélecteurs numériques pour les rôles à instances multiples du patron choisi.
     *
     * @param pattern patron de conception sélectionné
     */
    private void rebuildRolesControls(DesignPatternType pattern) {
        rolesPanel.removeAll();
        optionalRoleCheckBoxes.clear();
        manyRoleSpinners.clear();

        for (PatternRole role : pattern.getRoles()) {
            if (!role.required()) {
                if ("CLIENT".equals(role.id())) {
                    continue;
                }
                JBCheckBox cb = new JBCheckBox("Inclure " + role.id() + " (" + role.description() + ")", true);
                cb.addActionListener(e -> refreshPreview());
                optionalRoleCheckBoxes.put(role.id(), cb);
                rolesPanel.add(cb);
            }

            if (role.multiple()) {
                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
                JBIntSpinner spinner = new JBIntSpinner(role.defaultInstances(), 1, 10, 1);
                spinner.addChangeListener(e -> refreshPreview());
                manyRoleSpinners.put(role.id(), spinner);

                row.add(new JBLabel("Nombre de " + role.id() + " :"));
                row.add(spinner);
                rolesPanel.add(row);
            }
        }
    }

    /**
     * Recalcule et affiche en direct la liste des fichiers Java qui seront générés
     * selon le nom de base, les variantes saisies, les rôles cochés et l'option de sous-package.
     */
    @SuppressWarnings("unchecked")
    private void refreshPreview() {
        DesignPatternType pattern = getSelectedPattern();
        if (pattern == null) return;

        String subPkg = pattern.getSubPackage();
        String fullPkgPreview = targetPackageName.isEmpty() ? subPkg : targetPackageName + "." + subPkg;
        subPackageCheckBox.setText("Créer un sous-package (" + fullPkgPreview + ")");

        String baseName = nameField.getText().trim().isEmpty() ? "MyPattern" : nameField.getText().trim();
        String prefix = subPackageCheckBox.isSelected() ? subPkg + "/" : "";

        List<String> variants = List.of();
        if (currentConfigPanel != null && currentConfigPanel.getOptions().get("variants") instanceof List<?> list) {
            variants = (List<String>) list;
        }

        boolean isInjectingClient = injectClientCheckBox.isVisible() && injectClientCheckBox.isSelected();
        String clientRoleId = pattern.findClientRole().map(PatternRole::id).orElse(null);

        previewListModel.clear();
        for (PatternRole role : pattern.getRoles()) {
            if (role.id().equals(clientRoleId) && isInjectingClient) {
                continue;
            }

            if (!role.required()) {
                if ("CLIENT".equals(role.id())) {
                    if (!includeClientCheckBox.isSelected()) continue;
                } else {
                    JBCheckBox cb = optionalRoleCheckBoxes.get(role.id());
                    if (cb != null && !cb.isSelected()) continue;
                }
            }

            if (role.multiple()) {
                int count = manyRoleSpinners.containsKey(role.id())
                        ? manyRoleSpinners.get(role.id()).getNumber()
                        : role.defaultInstances();

                for (int i = 1; i <= count; i++) {
                    String resolved = resolveRoleNameWithVariants(role, baseName, i, variants);
                    previewListModel.addElement(new PreviewItem(prefix + resolved + ".java", role.kind()));
                }
            } else {
                String resolved = role.resolveName(baseName, 1);
                previewListModel.addElement(new PreviewItem(prefix + resolved + ".java", role.kind()));
            }
        }
        if (isInjectingClient){
            previewListModel.addElement(new PreviewItem("Injection dans " + activeClassName + ".java", ClassKind.CLASS));
        }
    }

    /**
     * Résout le nom d'une classe multiple en substituant les placeholders {@code {Index}} ou {@code {Letter}}
     * par le nom de variante saisi par l'utilisateur s'il est disponible.
     *
     * @param role     rôle participant concerné
     * @param baseName nom de base saisi
     * @param index    index de l'instance (à partir de 1)
     * @param variants liste des variantes saisies par l'utilisateur
     * @return nom de classe Java résolu
     */
    private String resolveRoleNameWithVariants(PatternRole role, String baseName, int index, List<String> variants) {
        if (!variants.isEmpty() && index <= variants.size()) {
            String variant = sanitizeIdentifierPart(variants.get(index - 1));
            if (!variant.isEmpty()) {
                return role.namePattern()
                        .replace("{Name}", baseName)
                        .replace("{Index}", variant)
                        .replace("{Letter}", variant);
            }
        }
        return role.resolveName(baseName, index);
    }

    /**
     * Nettoie une chaîne brute saisie par l'utilisateur pour en faire un segment d'identifiant Java
     * valide commençant par une majuscule.
     *
     * @param raw texte brut saisi pour une variante
     * @return segment d'identifiant nettoyé et capitalisé, ou chaîne vide si aucun caractère valide
     */
    private String sanitizeIdentifierPart(String raw) {
        String clean = raw.replaceAll("[^a-zA-Z0-9_]", "");
        if (clean.isEmpty()) return "";
        return Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
    }

    /**
     * Revalide la mise en page des panneaux dynamiques et redimensionne la fenêtre modale.
     */
    private void revalidateAndPack() {
        specificConfigContainer.revalidate();
        specificConfigContainer.repaint();
        rolesPanel.revalidate();
        rolesPanel.repaint();
        if (getWindow() != null && getWindow().isShowing()) {
            pack();
        }
    }

    /**
     * Construit et retourne le panneau central complet du formulaire avec la zone dynamique
     * et la liste d'aperçu des fichiers générés.
     *
     * @return composant Swing principal de la boîte de dialogue
     */
    @Override
    protected @Nullable JComponent createCenterPanel() {
        JBScrollPane previewScroll = new JBScrollPane(previewList);
        previewScroll.setPreferredSize(JBUI.size(440, 130));

        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Patron :", patternBox)
                .addComponent(descriptionLabel)
                .addLabeledComponent("Nom de base :", nameField)
                .addComponent(subPackageCheckBox)
                .addComponent(pedagogicalCheckBox)
                .addComponent(includeClientCheckBox)
                .addComponent(injectClientCheckBox)
                .addSeparator()
                .addComponent(specificConfigContainer)
                .addComponent(rolesPanel)
                .addSeparator()
                .addLabeledComponent("Aperçu des fichiers générés :", previewScroll, true)
                .getPanel();
    }

    /**
     * Définit le champ texte du nom de base comme composant recevant le focus à l'ouverture.
     *
     * @return le champ {@code nameField}
     */
    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return nameField;
    }

    /**
     * Valide la conformité syntaxique du nom de base (non vide, identifiant Java valide, PascalCase)
     * ainsi que la validité des options saisies dans le panneau spécifique actif.
     *
     * @return une {@link ValidationInfo} en cas d'erreur, ou {@code null} si tout est valide
     */
    @Override
    protected @Nullable ValidationInfo doValidate() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            return new ValidationInfo("Veuillez saisir un nom de base (ex: Payment)", nameField);
        }
        if (project != null && !PsiNameHelper.getInstance(project).isIdentifier(name)) {
            return new ValidationInfo("Le nom doit être un identifiant Java valide", nameField);
        }
        if (!Character.isUpperCase(name.charAt(0))) {
            return new ValidationInfo("Une classe Java doit commencer par une majuscule (PascalCase)", nameField);
        }
        return currentConfigPanel != null ? currentConfigPanel.validate() : null;
    }

    /**
     * Rassemble l'ensemble des choix effectués par l'utilisateur dans un objet de configuration immuable.
     *
     * @return configuration complète prête à être transmise au générateur
     */
    @SuppressWarnings("unchecked")
    public PatternGenerationConfig getConfig() {
        Set<String> enabledOptionals = new LinkedHashSet<>();
        optionalRoleCheckBoxes.forEach((id, cb) -> {
            if (cb.isSelected()) enabledOptionals.add(id);
        });

        Map<String, Integer> instanceCounts = new LinkedHashMap<>();
        manyRoleSpinners.forEach((id, spinner) -> instanceCounts.put(id, spinner.getNumber()));

        List<String> variants = List.of();
        if (currentConfigPanel != null && currentConfigPanel.getOptions().get("variants") instanceof List<?> list) {
            variants = (List<String>) list;
        }

        boolean injectClient = injectClientCheckBox.isVisible() && injectClientCheckBox.isSelected();
        if (injectClient) {
            enabledOptionals.add("CLIENT");
        }
        return new PatternGenerationConfig(
                getSelectedPattern(),
                getBaseName(),
                subPackageCheckBox.isSelected(),
                pedagogicalCheckBox.isSelected(),
                enabledOptionals,
                instanceCounts,
                variants,
                injectClient
        );
    }

    /**
     * Retourne le patron de conception actuellement sélectionné.
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
     * Indique si le mode pédagogique est activé.
     *
     * @return {@code true} si les commentaires pédagogiques doivent être générés
     */
    public boolean isPedagogical() {
        return pedagogicalCheckBox.isSelected();
    }

    /**
     * Indique si les fichiers doivent être placés dans un sous-package dédié au patron.
     *
     * @return {@code true} si la création du sous-package est cochée
     */
    public boolean isCreateSubPackage() {
        return subPackageCheckBox.isSelected();
    }

    /**
     * Représente une entrée affichée dans la liste de prévisualisation des fichiers à générer.
     *
     * @param displayPath chemin relatif du fichier affiché dans l'aperçu
     * @param kind        nature du type Java déterminant l'icône associée
     */
    private record PreviewItem(String displayPath, ClassKind kind) {}
}