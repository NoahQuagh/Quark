package com.noahquagh.quark.generator;

/**
 * Contrat commun de construction syntaxique d'un fichier source
 * (implémenté par {@code JavaClassBuilder} dans {@code jcb}, et futur {@code KotlinClassBuilder} dans {@code kcb}).
 *
 * @author noahq
 * @version 1.0
 */
public interface CodeBuilder {

    CodeBuilder withJavadoc(String javadoc);

    CodeBuilder withParent(String relationKeyword, String parentClassName);

    CodeBuilder addImport(String qualifiedClassName);

    CodeBuilder addField(String declaration);

    CodeBuilder addConstructor(String constructorCode);

    CodeBuilder addMethod(String methodCode);

    String build();
}