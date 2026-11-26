package com.noahquagh.quark.kcb;

import com.noahquagh.quark.generator.CodeBuilder;

public class KotlinClassBuilder implements CodeBuilder {
    @Override
    public CodeBuilder withJavadoc(String javadoc) {
        return null;
    }

    @Override
    public CodeBuilder withParent(String relationKeyword, String parentClassName) {
        return null;
    }

    @Override
    public CodeBuilder addImport(String qualifiedClassName) {
        return null;
    }

    @Override
    public CodeBuilder addField(String declaration) {
        return null;
    }

    @Override
    public CodeBuilder addConstructor(String constructorCode) {
        return null;
    }

    @Override
    public CodeBuilder addMethod(String methodCode) {
        return null;
    }

    @Override
    public String build() {
        return "";
    }
}
