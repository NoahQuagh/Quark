package com.noahquagh.quark.generator;

/**
 * Contrat commun de construction syntaxique d'un fichier source
 * (implémenté par {@code JavaClassBuilder} dans {@code jcb}, et futur {@code KotlinClassBuilder} dans {@code kcb}).
 *
 * @author noahq
 * @version 1.0
 */
public interface CodeBuilder {

    /**
     * Définit le bloc Javadoc placé au-dessus de la déclaration de classe.
     *
     * @param  javadoc {@code String} commentaire Javadoc formaté
     * @return l'instance courante du builder
     */
    CodeBuilder withJavadoc(String javadoc);

    /**
     * Configure la clause d'héritage ou d'implémentation du type.
     *
     * @param relationKeyword {@code String} mot-clé Java ({@code "extends"} ou {@code "implements"})
     * @param parentClassName {@code String} nom du type parent
     * @return l'instance courante du builder
     */
    CodeBuilder withParent(String relationKeyword, String parentClassName);

    /**
     * Ajoute une déclaration d'import sans doublon.
     *
     * @param qualifiedClassName {@code String} nom complet de la classe à importer (ex : {@code "java.util.List"})
     * @return l'instance courante du builder
     */
    CodeBuilder addImport(String qualifiedClassName);

    /**
     * Ajoute une déclaration d'attribut à la classe.
     *
     * @param declaration {@code String} ligne complète de déclaration de champ (avec point-virgule)
     * @return l'instance courante du builder
     */
    CodeBuilder addField(String declaration);

    /**
     * Ajoute un constructeur à la classe.
     *
     * @param constructorCode {@code String} bloc complet du constructeur
     * @return l'instance courante du builder
     */
    CodeBuilder addConstructor(String constructorCode);

    /**
     * Ajoute une méthode (signature ou implémentation) au type.
     *
     * @param methodCode {@code String} bloc complet de la méthode
     * @return l'instance courante du builder
     */
    CodeBuilder addMethod(String methodCode);

    /**
     * Assemble et retourne le code source Java complet prêt à être écrit.
     *
     * @return code source Java formaté
     */
    String build();
}