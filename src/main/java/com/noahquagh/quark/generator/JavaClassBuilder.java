package com.noahquagh.quark.generator;

import com.noahquagh.quark.model.ClassKind;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    public JavaClassBuilder(String packageName, ClassKind kind, String className) {
        this.packageName = packageName;
        this.kind = kind;
        this.className = className;
    }

    public JavaClassBuilder withJavadoc(String javadoc){
        this.javadoc = javadoc;
        return this;
    }

    public JavaClassBuilder withParent(String relationKeyword, String parentClassName){
        if (parentClassName != null && !parentClassName.isBlank()){
            this.inheritanceClause=" "+relationKeyword+" "+parentClassName;
        }
        return this;
    }

    public JavaClassBuilder addImport(String qualifiedClassName){
        imports.add(qualifiedClassName);
        return this;
    }

    public JavaClassBuilder addField(String declaration){
        fields.add(declaration);
        return this;
    }

    public JavaClassBuilder addConstructor(String constructorCode){
        constructors.add(constructorCode);
        return this;
    }

    public JavaClassBuilder addMethod(String methodCode){
        methods.add(methodCode);
        return this;
    }

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
