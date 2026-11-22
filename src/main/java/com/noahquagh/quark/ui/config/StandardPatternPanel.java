package com.noahquagh.quark.ui.config;

import com.intellij.ui.components.JBLabel;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Panneau de configuration par défaut utilisé pour les patrons de conception à structure fixe
 * ne nécessitant pas de saisie de variantes supplémentaires.
 *
 * @author noahq
 * @version 1.2
 */
public class StandardPatternPanel implements PatternConfigPanel {
    private final JPanel panel = new JPanel(new BorderLayout());

    /**
     * Initialise le panneau standard avec un message d'information indiquant l'absence d'options spécifiques.
     *
     * @param description description du patron (conservée pour compatibilité de signature avec les autres panneaux)
     */
    public StandardPatternPanel(String description) {
        JBLabel hint = new JBLabel("<html><body style='color: gray;'>Structure fixe standard (aucune variante supplémentaire requise).</body></html>");
        panel.add(hint, BorderLayout.CENTER);
    }

    /**
     * Retourne le composant Swing racine contenant le message d'information.
     *
     * @return panneau Swing du composant
     */
    @Override
    public JComponent getComponent() {
        return panel;
    }

    /**
     * Retourne une map vide car ce panneau ne comporte aucun paramètre additionnel.
     *
     * @return map immuable vide
     */
    @Override
    public Map<String, Object> getOptions() {
        return Map.of();
    }
}