package com.noahquagh.quark.ui.config;

import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.List;
import java.util.Arrays;
import java.util.Map;

/**
 * Panneau de configuration dynamique permettant à l'utilisateur de saisir les variantes
 * concrètes d'un patron de conception (ex : {@code "A, B"} ou {@code "PayPal, Stripe"})
 * séparées par des virgules.
 *
 * <p>Chaque modification du champ texte notifie automatiquement la boîte de dialogue parente
 * via {@link #setOnChangeListener(Runnable)} afin de rafraîchir l'aperçu des classes générées.</p>
 *
 * @author noahq
 * @version 1.1
 */
public class MultiVariantPatternPanel implements PatternConfigPanel{

    private final JPanel panel = new JPanel(new BorderLayout(8, 8));
    private final JBTextField variantsField = new JBTextField("A, B");
    private Runnable onChangeListener;

    /**
     * Initialise le panneau avec son libellé descriptif et configure l'écouteur de saisie
     * en temps réel sur le champ des variantes.
     *
     * @param labelText libellé affiché à gauche du champ texte (ex : {@code "Variantes (ex: A, B) :"})
     */
    public MultiVariantPatternPanel(String labelText) {
        panel.add(new JBLabel(labelText), BorderLayout.WEST);
        panel.add(variantsField, BorderLayout.CENTER);

        variantsField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                if (onChangeListener != null) {
                    onChangeListener.run();
                }
            }
        });
    }

    @Override
    public JComponent getComponent() {
        return panel;
    }

    @Override
    public ValidationInfo validate() {
        if (variantsField.getText().isBlank()) {
            return new ValidationInfo("Indique au moins une variante", variantsField);
        }
        return null;
    }

    @Override
    public Map<String, Object> getOptions() {
        List<String> variants = Arrays.stream(variantsField.getText().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return Map.of("variants", variants);
    }

    @Override
    public void setOnChangeListener(Runnable onChange) {
        this.onChangeListener = onChange;
    }
}
