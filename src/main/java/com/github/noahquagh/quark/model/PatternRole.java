package com.github.noahquagh.quark.model;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Un participant d'un design pattern (ex : "Strategy", "ConcreteStrategy", "Context").
 *
 * <p>Placeholders utilisables dans {@code namePattern} :
 * <ul>
 *   <li>{@code {Name}}   : nom de base saisi par l'utilisateur (ex : "Payment")</li>
 *   <li>{@code {Index}}  : 1, 2, 3... (rôles multiples)</li>
 *   <li>{@code {Letter}} : A, B, C... (rôles multiples)</li>
 * </ul>
 *
 * <p>Placeholders utilisables dans {@code methods} :
 * <ul>
 *   <li>{@code {Name}}    : nom de base</li>
 *   <li>{@code {ROLE_ID}} : nom de classe généré pour le rôle ciblé
 *       (si ce rôle est multiple, la méthode est générée une fois par instance)</li>
 * </ul>
 * Une méthode d'une classe abstraite est préfixée par "abstract " si elle est abstraite.
 *
 * @param id               identifiant du rôle, unique dans le pattern (ex : "CONCRETE_STRATEGY")
 * @param namePattern      modèle du nom de classe
 * @param kind             interface / classe abstraite / classe
 * @param description      rôle du participant (affichable dans l'IDE)
 * @param methods          signatures de méthodes à générer
 * @param parentId         id du rôle étendu/implémenté, ou null
 * @param usesIds          ids des rôles référencés (champs / dépendances)
 * @param defaultInstances nombre d'instances générées par défaut (1 = rôle unique)
 * @param required         false si le rôle peut être décoché dans l'assistant
 *
 * @author noahq
 * @version 1.2
 */
public record PatternRole(
        String id,
        String namePattern,
        ClassKind kind,
        String description,
        List<String> methods,
        String parentId,
        List<String> usesIds,
        int defaultInstances,
        boolean required
) {

    // Fabriques

    public static PatternRole iface(String id, String namePattern, String description, String... methods) {
        return new PatternRole(id, namePattern, ClassKind.INTERFACE, description,
                List.of(methods), null, List.of(), 1, true);
    }

    public static PatternRole abstractClass(String id, String namePattern, String description, String... methods) {
        return new PatternRole(id, namePattern, ClassKind.ABSTRACT_CLASS, description,
                List.of(methods), null, List.of(), 1, true);
    }

    public static PatternRole concreteClass(String id, String namePattern, String description, String... methods) {
        return new PatternRole(id, namePattern, ClassKind.CLASS, description,
                List.of(methods), null, List.of(), 1, true);
    }

    // Modificateurs

    /** Ce rôle étend / implémente le rôle {@code parentId}. */
    public PatternRole extending(String parentId) {
        return new PatternRole(id, namePattern, kind, description, methods, parentId, usesIds, defaultInstances, required);
    }

    /** Ce rôle référence d'autres rôles (champ ou dépendance). */
    public PatternRole uses(String... ids) {
        return new PatternRole(id, namePattern, kind, description, methods, parentId, List.of(ids), defaultInstances, required);
    }

    /** Rôle généré en 2 exemplaires par défaut. */
    public PatternRole many() {
        return many(2);
    }

    public PatternRole many(int count) {
        return new PatternRole(id, namePattern, kind, description, methods, parentId, usesIds, count, required);
    }

    /** Rôle facultatif dans l'assistant de génération. */
    public PatternRole optional() {
        return new PatternRole(id, namePattern, kind, description, methods, parentId, usesIds, defaultInstances, false);
    }

    // Utilitaires

    public boolean multiple() {
        return defaultInstances > 1;
    }

    /**
     * Résout le nom de classe.
     *
     * @param baseName nom saisi par l'utilisateur
     * @param index    numéro de l'instance (à partir de 1)
     */
    public String resolveName(String baseName, int index) {
        return namePattern
                .replace("{Name}", baseName)
                .replace("{Index}", String.valueOf(index))
                .replace("{Letter}", String.valueOf((char) ('A' + index - 1)));
    }

    public List<String> resolveSuffixes() {
        return IntStream.rangeClosed(1, Math.max(1, defaultInstances))
                .mapToObj(i -> resolveName("", i))
                .toList();
    }
}