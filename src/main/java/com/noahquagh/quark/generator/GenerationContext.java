package com.noahquagh.quark.generator;

import com.noahquagh.quark.model.DesignPatternType;

import java.util.List;
import java.util.Map;


/**
 * Contexte immuable regroupant l'ensemble des paramètres et résolutions de noms d'une session de génération.
 *
 * @param pattern     patron de conception sélectionné
 * @param baseName    nom de base saisi par l'utilisateur (ex : {@code "Payment"})
 * @param packageName nom qualifié du package cible
 * @param roleClasses correspondance entre chaque identifiant de rôle et les noms de classes générés
 * @param pedagogical {@code true} si les commentaires pédagogiques doivent être générés
 *
 * @author noahq
 * @version 1.2
 */
public record GenerationContext(
        DesignPatternType pattern,
        String baseName,
        String packageName,
        Map<String, List<String>> roleClasses,
        boolean pedagogical
) {
    /**
     * Retourne le nom de la première classe générée pour un identifiant de rôle donné.
     *
     * @param roleId identifiant du rôle recherché (ex : {@code "STRATEGY"})
     * @return nom de la classe principale associée, ou {@code null} si absent
     */
    public String primaryClassOf(String roleId){
        List<String> names = roleClasses.get(roleId);
        return (names != null && !names.isEmpty()) ? names.get(0) : null;
    }

    /**
     * Retourne toutes les classes générées pour un identifiant de rôle donné.
     *
     * @param roleId identifiant du rôle recherché
     * @return liste des noms de classes générés, ou liste vide si absent
     */
    public List<String> allClassesOf(String roleId) {
        return roleClasses.getOrDefault(roleId, List.of());
    }
}
