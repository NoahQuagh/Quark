package com.noahquagh.quark.generator.strategy;

import com.noahquagh.quark.generator.GenerationContext;
import com.noahquagh.quark.generator.JavaClassBuilder;
import com.noahquagh.quark.model.PatternRole;

public interface PatternStrategy {
    void buildRole(PatternRole role, String className, JavaClassBuilder builder, GenerationContext ctx);
}
