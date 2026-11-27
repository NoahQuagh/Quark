package com.noahquagh.quark.kcb.strategy;

import com.noahquagh.quark.generator.CodeBuilder;
import com.noahquagh.quark.generator.GenerationContext;
import com.noahquagh.quark.generator.PatternStrategy;
import com.noahquagh.quark.kcb.KotlinClassBuilder;
import com.noahquagh.quark.model.PatternRole;

/**
 * Stratégie spécialisée pour le patron {@code SINGLETON} en Kotlin.
 * Utilise le mot-clé natif {@code object} idiomatique et thread-safe.
 *
 * @author noahq
 * @version 1.0
 */
public class KotlinSingletonStrategy implements PatternStrategy {

    @Override
    public void buildRole(PatternRole role, String className, CodeBuilder builder, GenerationContext ctx) {
        if (ctx.pedagogical()) {
            builder.withJavadoc("""
                    /**
                     * Pattern Singleton : garantit une instance unique de %s via le mot-clé Kotlin object.
                     */""".formatted(className));
        }

        if (builder instanceof KotlinClassBuilder ktBuilder) {
            ktBuilder.asObject();
        }
    }
}
