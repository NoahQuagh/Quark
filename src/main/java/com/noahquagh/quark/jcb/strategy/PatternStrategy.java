package com.noahquagh.quark.jcb.strategy;

import com.noahquagh.quark.jcb.GenerationContext;
import com.noahquagh.quark.jcb.JavaClassBuilder;
import com.noahquagh.quark.model.PatternRole;

/**
 * Contrat définissant la stratégie de construction du code source pour un rôle d'un design pattern.
 *
 * @author noahq
 * @version 1.0
 */
public interface PatternStrategy {
    /**
     * Peuple le {@link JavaClassBuilder} avec la structure (Javadoc, héritage, champs, constructeurs et méthodes)
     * correspondant au rôle fourni.
     *
     * @param role      participant du design pattern à générer
     * @param className nom exact de la classe ou interface générée
     * @param builder   constructeur fluide du fichier source Java
     * @param ctx       contexte global de la génération
     */
    void buildRole(PatternRole role, String className, JavaClassBuilder builder, GenerationContext ctx);
}
