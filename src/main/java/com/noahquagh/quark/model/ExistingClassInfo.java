package com.noahquagh.quark.model;

import java.util.List;

/**
 * Représentation immuable et indépendante du SDK IntelliJ d'une classe ou interface existante
 * sélectionnée par l'utilisateur pour être adaptée, décorée, proxifiée ou encapsulée.
 *
 * @param qualifiedName nom qualifié complet (ex : {@code "com.app.legacy.LegacyPrinter"})
 * @param simpleName    nom simple de la classe (ex : {@code "LegacyPrinter"})
 * @param packageName   nom du package d'origine (ex : {@code "com.app.legacy"})
 * @param isInterface   {@code true} si le type cible est une interface
 * @param methods       liste des signatures de méthodes publiques exploitables
 *
 * @author noahq
 * @version 1.0
 */
public record ExistingClassInfo(
        String qualifiedName,
        String simpleName,
        String packageName,
        boolean isInterface,
        List<MethodSignature> methods
) {
    /**
     * Représente la signature simplifiée d'une méthode publique extraite via PSI.
     *
     * @param returnType type de retour (ex : {@code "void"}, {@code "String"})
     * @param name       nom de la méthode (ex : {@code "printDocument"})
     * @param parameters liste des paramètres (ex : {@code ["String text", "int copies"]})
     */
    public record MethodSignature(
            String returnType,
            String name,
            List<String> parameters
    ) {
        /**
         * Formate la déclaration complète de la méthode (ex : {@code "void printDocument(String text, int copies)"}).
         */
        public String toDeclaration() {
            return returnType + " " + name + "(" + String.join(", ", parameters) + ")";
        }

        /**
         * Extrait uniquement les noms des arguments pour générer l'appel délégué
         * (ex : {@code "text, copies"}).
         */
        public String toCallArguments() {
            return parameters.stream()
                    .map(p -> {
                        String[] parts = p.trim().split("\\s+");
                        return parts[parts.length - 1];
                    })
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
        }
    }
}