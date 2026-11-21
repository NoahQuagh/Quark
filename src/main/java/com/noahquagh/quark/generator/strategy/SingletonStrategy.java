package com.noahquagh.quark.generator.strategy;

import com.noahquagh.quark.generator.GenerationContext;
import com.noahquagh.quark.generator.JavaClassBuilder;
import com.noahquagh.quark.model.PatternRole;

/**
 * Stratégie spécialisée pour le patron {@code SINGLETON}.
 * Génère une implémentation thread-safe basée sur le verrouillage à double vérification.
 *
 * @author noahq
 * @version 1.1
 */
public class SingletonStrategy implements PatternStrategy {


    /**
     * Ajoute l'attribut {@code instance} volatile, le constructeur privé et la méthode statique {@code getInstance()}.
     *
     * @param role      participant du design pattern à générer
     * @param className nom exact de la classe Singleton générée
     * @param builder   constructeur fluide du fichier source Java
     * @param ctx       contexte global de la génération
     */
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
