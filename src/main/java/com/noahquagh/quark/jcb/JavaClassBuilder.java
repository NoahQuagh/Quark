package com.noahquagh.quark.jcb;

import com.noahquagh.quark.generator.CodeBuilder;
import com.noahquagh.quark.model.ClassKind;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Constructeur fluide permettant d'assembler syntaxiquement un fichier source Java.
 *
 * @author noahq
 * @version 1.2
 */
public class JavaClassBuilder implements CodeBuilder {
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

    @Override
    public CodeBuilder withJavadoc(String javadoc){
        this.javadoc = javadoc;
        return this;
    }

    @Override
    public CodeBuilder withParent(String relationKeyword, String parentClassName){
        if (parentClassName != null && !parentClassName.isBlank()){
            this.inheritanceClause=" "+relationKeyword+" "+parentClassName;
        }
        return this;
    }

    @Override
    public CodeBuilder addImport(String qualifiedClassName){
        imports.add(qualifiedClassName);
        return this;
    }


    @Override
    public CodeBuilder addField(String declaration){
        fields.add(declaration);
        return this;
    }


    @Override
    public CodeBuilder addConstructor(String constructorCode){
        constructors.add(constructorCode);
        return this;
    }


    @Override
    public CodeBuilder addMethod(String methodCode){
        methods.add(methodCode);
        return this;
    }


    @Override
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
