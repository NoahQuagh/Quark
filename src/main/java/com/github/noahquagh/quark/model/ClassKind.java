package com.github.noahquagh.quark.model;

/**
 * Nature du fichier Java généré pour un rôle de design pattern.
 *
 * @author noahq
 * @version 1.2
 */
public enum ClassKind {
    INTERFACE("interface"),
    ABSTRACT_CLASS("abstract class"),
    CLASS("class");

    private final String keyword;

    ClassKind(String keyword) {
        this.keyword = keyword;
    }

    /** Mot-clé Java à utiliser dans la déclaration (ex : "abstract class"). */
    public String getKeyword() {
        return keyword;
    }
}