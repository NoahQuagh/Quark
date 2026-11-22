package com.noahquagh.quark.ui.config;

import com.intellij.openapi.ui.ValidationInfo;

import javax.swing.*;
import java.util.Map;

/**
 * Interface représentant la section dynamique de la boite de dialogue
 *
 * @author noahq
 * @version 1.1
 */
public interface PatternConfigPanel {

    /**
     * Composant Swing à afficher dans la zone dynamique du dialogue
     *
     * @return Composant Swing
     */
    JComponent getComponent();

    /**
     * Valide les champs spécifiques au pattern avant le clic sur OK
     *
     * @return valide les informations
     */
    default ValidationInfo validate(){
        return null;
    }

    /**
     * Retourne les options saisies par l'utilisateur
     *
     * @return {@code Map<String,Object>} option choisie
     */
    Map<String,Object> getOptions();

    /**
     * Permet au panneau spécifique de notifier le dialogue parent d'un changement
     *
     * @param onChange événement
     */
    default void setOnChangeListener(Runnable onChange) {}
}
