package com.noahquagh.quark.generator.strategy;

import com.noahquagh.quark.generator.GenerationContext;
import com.noahquagh.quark.generator.JavaClassBuilder;
import com.noahquagh.quark.model.PatternRole;

public class SingletonStrategy implements PatternStrategy {


    @Override
    public void buildRole(PatternRole role, String className, JavaClassBuilder builder, GenerationContext ctx) {
        if (ctx.pedagogical()) {
            builder.withJavadoc("""
                    /**
                     * Pattern Singleton : garantit une instance unique de %s.
                     */""".formatted(className));
        }

        builder.addField("private static volatile " + className + " instance;")
                .addConstructor("private " + className + "() {\n        // Constructeur privé\n    }")
                .addMethod("""
                           public static %s getInstance() {
                               if (instance == null) {
                                   synchronized (%s.class) {
                                       if (instance == null) {
                                           instance = new %s();
                                       }
                                   }
                               }
                               return instance;
                           }""".formatted(className, className, className));
    }
}
