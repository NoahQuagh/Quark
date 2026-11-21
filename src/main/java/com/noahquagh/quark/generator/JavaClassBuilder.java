package com.noahquagh.quark.generator;

import com.noahquagh.quark.model.ClassKind;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Constructeur fluide permettant d'assembler syntaxiquement un fichier source Java.
 *
 * @author noahq
 * @version 1.1
 */
public class JavaClassBuilder {
    private final String packageName;
    private final ClassKind kind;
    private final String className;

    private final Set<String> imports = new LinkedHashSet<>();
    private final List<String> fields = new ArrayList<>();
    private final List<String> constructors = new ArrayList<>();
    private final List<String> methods = new ArrayList<>();

    private String javadoc;
    private String inheritanceClause = "";

    /**
     * Initialise un nouveau constructeur de fichier Java.
     *
     * @param packageName nom qualifié du package (vide si package par défaut)
     * @param kind        nature du type Java ({@code interface}, {@code abstract class}, {@code class})
     * @param className   nom du type Java à générer
     */
    public JavaClassBuilder(String packageName, ClassKind kind, String className) {
        this.packageName = packageName;
        this.kind = kind;
        this.className = className;
    }

    /**
     * Définit le bloc Javadoc placé au-dessus de la déclaration de classe.
     *
     * @param javadoc commentaire Javadoc formaté
     * @return l'instance courante du builder
     */
    public JavaClassBuilder withJavadoc(String javadoc){
        this.javadoc = javadoc;
        return this;
    }

    /**
     * Configure la clause d'héritage ou d'implémentation du type.
     *
     * @param relationKeyword mot-clé Java ({@code "extends"} ou {@code "implements"})
     * @param parentClassName nom du type parent
     * @return l'instance courante du builder
     */
    public JavaClassBuilder withParent(String relationKeyword, String parentClassName){
        if (parentClassName != null && !parentClassName.isBlank()){
            this.inheritanceClause=" "+relationKeyword+" "+parentClassName;
        }
        return this;
    }

    /**
     * Ajoute une déclaration d'import sans doublon.
     *
     * @param qualifiedClassName nom complet de la classe à importer (ex : {@code "java.util.List"})
     * @return l'instance courante du builder
     */
    public JavaClassBuilder addImport(String qualifiedClassName){
        imports.add(qualifiedClassName);
        return this;
    }

    /**
     * Ajoute une déclaration d'attribut à la classe.
     *
     * @param declaration ligne complète de déclaration de champ (avec point-virgule)
     * @return l'instance courante du builder
     */
    public JavaClassBuilder addField(String declaration){
        fields.add(declaration);
        return this;
    }

    /**
     * Ajoute un constructeur à la classe.
     *
     * @param constructorCode bloc complet du constructeur
     * @return l'instance courante du builder
     */
    public JavaClassBuilder addConstructor(String constructorCode){
        constructors.add(constructorCode);
        return this;
    }

    /**
     * Ajoute une méthode (signature ou implémentation) au type.
     *
     * @param methodCode bloc complet de la méthode
     * @return l'instance courante du builder
     */
    public JavaClassBuilder addMethod(String methodCode){
        methods.add(methodCode);
        return this;
    }

    /**
     * Assemble et retourne le code source Java complet prêt à être écrit.
     *
     * @return code source Java formaté
     */
    public String build(){
        StringBuilder sb = new StringBuilder();

        if (packageName != null && !packageName.isEmpty()) {
            sb.append("package ").append(packageName).append(";\n\n");
        }
        if (!imports.isEmpty()) {
            for (String imp : imports) {
                sb.append("import ").append(imp).append(";\n");
            }
            sb.append("\n");
        }
        if (javadoc != null && !javadoc.isBlank()) {
            sb.append(javadoc).append("\n");
        }

        sb.append("public ").append(kind.getKeyword()).append(" ").append(className)
                .append(inheritanceClause).append(" {\n\n");

        if (!fields.isEmpty()) {
            for (String field : fields) {
                sb.append("    ").append(field).append("\n");
            }
            sb.append("\n");
        }
        for (String ctor : constructors) {
            sb.append(ctor).append("\n\n");
        }
        for (String method : methods) {
            sb.append(method).append("\n\n");
        }

        sb.append("}\n");
        return sb.toString();
    }
}
