package com.noahquagh.quark.model;

/**
 * Nature du fichier Java généré pour un rôle de design pattern.
 *
 * @author noahq
 * @version 1.2
 */
public enum ClassKind {
    /** Déclare une interface Java ({@code interface}). */
    INTERFACE("interface"),
    /** Déclare une classe abstraite Java ({@code abstract class}). */
    ABSTRACT_CLASS("abstract class"),
    /** Déclare une classe concrète Java ({@code class}). */
    CLASS("class");

    private final String keyword;

    /**
     * Initialise une nature de type Java avec son mot-clé syntaxique associé.
     *
     * @param keyword mot-clé Java utilisé lors de la génération de l'en-tête
     */
    ClassKind(String keyword) {
        this.keyword = keyword;
    }

    /**
     * Retourne le mot-clé Java à utiliser dans la déclaration (ex : {@code "abstract class"}).
     *
     * @return mot-clé syntaxique Java correspondant
     */
    public String getKeyword() {
        return keyword;
    }
}