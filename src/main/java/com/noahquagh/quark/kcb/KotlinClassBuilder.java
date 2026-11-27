package com.noahquagh.quark.kcb;

import com.noahquagh.quark.generator.CodeBuilder;
import com.noahquagh.quark.model.ClassKind;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Constructeur fluide permettant d'assembler syntaxiquement un fichier source Kotlin (.kt).
 *
 * @author noahq
 * @version 1.0
 */
public class KotlinClassBuilder implements CodeBuilder {

    private final String packageName;
    private final ClassKind kind;
    private final String className;

    private final Set<String> imports = new LinkedHashSet<>();
    private final List<String> fields = new ArrayList<>();
    private final List<String> constructors = new ArrayList<>();
    private final List<String> methods = new ArrayList<>();

    private String javadoc;
    private String inheritanceClause = "";
    private boolean isObject = false;
    private boolean isOpen = false;

    public KotlinClassBuilder(String packageName, ClassKind kind, String className) {
        this.packageName = packageName;
        this.kind = kind;
        this.className = className;
    }

    /** Permet de générer un {@code object Name} (utilisé pour le Singleton idiomatique Kotlin). */
    public KotlinClassBuilder asObject() {
        this.isObject = true;
        return this;
    }

    /** Marque la classe comme {@code open class} afin qu'elle puisse être étendue en Kotlin. */
    public KotlinClassBuilder asOpen() {
        this.isOpen = true;
        return this;
    }

    @Override
    public CodeBuilder withJavadoc(String javadoc) {
        this.javadoc = javadoc;
        return this;
    }

    @Override
    public CodeBuilder withParent(String relationKeyword, String parentClassName) {
        if (parentClassName != null && !parentClassName.isBlank()) {
            // En Kotlin : ": ParentClass()" si héritage de classe, ": ParentInterface" si interface
            String suffix = "extends".equals(relationKeyword) ? "()" : "";
            this.inheritanceClause = " : " + parentClassName + suffix;
        }
        return this;
    }

    @Override
    public CodeBuilder addImport(String qualifiedClassName) {
        imports.add(qualifiedClassName);
        return this;
    }

    @Override
    public CodeBuilder addField(String declaration) {
        fields.add(declaration);
        return this;
    }

    @Override
    public CodeBuilder addConstructor(String constructorCode) {
        constructors.add(constructorCode);
        return this;
    }

    @Override
    public CodeBuilder addMethod(String methodCode) {
        methods.add(methodCode);
        return this;
    }

    @Override
    public String build() {
        StringBuilder sb = new StringBuilder();

        if (packageName != null && !packageName.isEmpty()) {
            sb.append("package ").append(packageName).append("\n\n");
        }
        if (!imports.isEmpty()) {
            for (String imp : imports) {
                sb.append("import ").append(imp).append("\n");
            }
            sb.append("\n");
        }
        if (javadoc != null && !javadoc.isBlank()) {
            sb.append(javadoc).append("\n");
        }

        String headerKeyword = resolveHeaderKeyword();
        sb.append(headerKeyword).append(" ").append(className)
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

    private String resolveHeaderKeyword() {
        if (isObject) return "object";
        return switch (kind) {
            case INTERFACE -> "interface";
            case ABSTRACT_CLASS -> "abstract class";
            case CLASS -> isOpen ? "open class" : "class";
        };
    }
}
