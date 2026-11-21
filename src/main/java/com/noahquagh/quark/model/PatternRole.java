package com.noahquagh.quark.model;

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

    /**
     * Crée un participant de type {@link ClassKind#INTERFACE}.
     *
     * @param id          identifiant unique du rôle dans le pattern
     * @param namePattern modèle de nommage de l'interface (ex : {@code "{Name}Strategy"})
     * @param description description pédagogique du rôle
     * @param methods     signatures des méthodes déclarées par l'interface
     * @return une nouvelle instance immuable de {@link PatternRole}
     */
    public static PatternRole iface(String id, String namePattern, String description, String... methods) {
        return new PatternRole(id, namePattern, ClassKind.INTERFACE, description,
                List.of(methods), null, List.of(), 1, true);
    }

    /**
     * Crée un participant de type {@link ClassKind#ABSTRACT_CLASS}.
     *
     * @param id          identifiant unique du rôle dans le pattern
     * @param namePattern modèle de nommage de la classe abstraite
     * @param description description pédagogique du rôle
     * @param methods     signatures des méthodes (préfixées par {@code "abstract "} si abstraites)
     * @return une nouvelle instance immuable de {@link PatternRole}
     */
    public static PatternRole abstractClass(String id, String namePattern, String description, String... methods) {
        return new PatternRole(id, namePattern, ClassKind.ABSTRACT_CLASS, description,
                List.of(methods), null, List.of(), 1, true);
    }

    /**
     * Crée un participant de type {@link ClassKind#CLASS}.
     *
     * @param id          identifiant unique du rôle dans le pattern
     * @param namePattern modèle de nommage de la classe concrète
     * @param description description pédagogique du rôle
     * @param methods     signatures des méthodes propres à la classe
     * @return une nouvelle instance immuable de {@link PatternRole}
     */
    public static PatternRole concreteClass(String id, String namePattern, String description, String... methods) {
        return new PatternRole(id, namePattern, ClassKind.CLASS, description,
                List.of(methods), null, List.of(), 1, true);
    }

    // Modificateurs

    /**
     * Retourne une copie du rôle en définissant le participant parent qu'il étend ou implémente.
     *
     * @param parentId identifiant du rôle parent
     * @return nouvelle instance de {@link PatternRole} avec {@code parentId} renseigné
     */
    public PatternRole extending(String parentId) {
        return new PatternRole(id, namePattern, kind, description, methods, parentId, usesIds, defaultInstances, required);
    }

    /**
     * Retourne une copie du rôle en déclarant les rôles dont il dépend (champs / associations).
     *
     * @param ids identifiants des rôles référencés
     * @return nouvelle instance de {@link PatternRole} avec {@code usesIds} renseigné
     */
    public PatternRole uses(String... ids) {
        return new PatternRole(id, namePattern, kind, description, methods, parentId, List.of(ids), defaultInstances, required);
    }

    /**
     * Retourne une copie du rôle configurée pour générer 2 exemplaires concrets par défaut.
     *
     * @return nouvelle instance de {@link PatternRole} avec {@code defaultInstances = 2}
     */
    public PatternRole many() {
        return many(2);
    }

    /**
     * Retourne une copie du rôle configurée pour générer un nombre donné d'exemplaires concrets.
     *
     * @param count nombre d'instances à générer
     * @return nouvelle instance de {@link PatternRole} avec {@code defaultInstances = count}
     */
    public PatternRole many(int count) {
        return new PatternRole(id, namePattern, kind, description, methods, parentId, usesIds, count, required);
    }

    /**
     * Retourne une copie du rôle marquée comme facultative ({@code required = false}).
     *
     * @return nouvelle instance de {@link PatternRole} facultative
     */
    public PatternRole optional() {
        return new PatternRole(id, namePattern, kind, description, methods, parentId, usesIds, defaultInstances, false);
    }

    // Utilitaires

    /**
     * Indique si ce rôle génère plusieurs classes concrètes.
     *
     * @return {@code true} si {@code defaultInstances > 1}, {@code false} sinon
     */
    public boolean multiple() {
        return defaultInstances > 1;
    }

    /**
     * Résout le nom final de la classe en remplaçant les placeholders {@code {Name}}, {@code {Index}} et {@code {Letter}}.
     *
     * @param baseName nom de base saisi par l'utilisateur
     * @param index    numéro de l'instance (à partir de 1)
     * @return nom de classe Java résolu
     */
    public String resolveName(String baseName, int index) {
        return namePattern
                .replace("{Name}", baseName)
                .replace("{Index}", String.valueOf(index))
                .replace("{Letter}", String.valueOf((char) ('A' + index - 1)));
    }

    /**
     * Génère la liste des suffixes (ou noms sans base) produits pour toutes les instances de ce rôle.
     *
     * @return liste des suffixes résolus
     */
    public List<String> resolveSuffixes() {
        return IntStream.rangeClosed(1, Math.max(1, defaultInstances))
                .mapToObj(i -> resolveName("", i))
                .toList();
    }
}