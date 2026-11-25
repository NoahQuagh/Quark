package com.noahquagh.quark.model;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Objet de transfert immuable regroupant l'ensemble des paramètres choisis par l'utilisateur
 * dans l'assistant de génération d'un patron de conception.
 *
 * @param pattern                patron de conception GoF sélectionné
 * @param baseName               nom de base en PascalCase utilisé pour préfixer les classes générées
 * @param createSubPackage       {@code true} pour isoler les fichiers générés dans un sous-package dédié
 * @param pedagogical            {@code true} pour inclure les commentaires Javadoc explicatifs et les TODO
 * @param enabledOptionalRoleIds identifiants des rôles facultatifs conservés par l'utilisateur
 * @param roleInstanceCounts     nombre d'instances configuré par identifiant de rôle multiple
 * @param variants               liste ordonnée des noms de variantes personnalisées (ex : {@code ["PayPal", "Stripe"]})
 * @param injectClientIntoActiveClass {@code true} pour injecter le client dans la classe ouverte au lieu de créer un fichier Client
 *
 * @author noahq
 * @version 1.1
 */
public record PatternGenerationConfig(
        DesignPatternType pattern,
        String baseName,
        boolean createSubPackage,
        boolean pedagogical,
        Set<String> enabledOptionalRoleIds,
        Map<String, Integer> roleInstanceCounts,
        List<String> variants,
        boolean injectClientIntoActiveClass,
        @Nullable ExistingClassInfo existingClassInfo
) {
    /**
     * Constructeur de compatibilité désactivant par défaut l'injection dans une classe active.
     */
    public PatternGenerationConfig(
            DesignPatternType pattern,
            String baseName,
            boolean createSubPackage,
            boolean pedagogical,
            Set<String> enabledOptionalRoleIds,
            Map<String, Integer> roleInstanceCounts,
            List<String> variants
    ) {
        this(pattern, baseName, createSubPackage, pedagogical, enabledOptionalRoleIds, roleInstanceCounts, variants, false,null);
    }
}