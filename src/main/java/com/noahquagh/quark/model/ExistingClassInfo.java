package com.noahquagh.quark.model;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Descripteur immuable et indépendant du SDK IntelliJ d'une classe existante du projet.
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
    public record MethodSignature(
            String returnType,
            String name,
            List<String> parameters
    ) {
        public String toDeclaration() {
            return returnType + " " + name + "(" + String.join(", ", parameters) + ")";
        }

        public String toCallArguments() {
            return parameters.stream()
                    .map(p -> {
                        String[] parts = p.trim().split("\\s+");
                        return parts[parts.length - 1];
                    })
                    .collect(Collectors.joining(", "));
        }
    }
}
