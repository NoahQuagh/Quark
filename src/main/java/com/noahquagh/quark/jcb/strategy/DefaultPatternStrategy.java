package com.noahquagh.quark.jcb.strategy;

import com.noahquagh.quark.generator.CodeBuilder;
import com.noahquagh.quark.generator.GenerationContext;
import com.noahquagh.quark.generator.MethodCollector;
import com.noahquagh.quark.generator.PatternStrategy;
import com.noahquagh.quark.model.ClassKind;
import com.noahquagh.quark.model.ExistingClassInfo;
import com.noahquagh.quark.model.ExistingClassMode;
import com.noahquagh.quark.model.PatternRole;

/**
 * Stratégie de génération générique pilotée par les métadonnées des {@link PatternRole}.
 * Gère l'héritage, les dépendances, la résolution des placeholders et l'implémentation des méthodes héritées.
 *
 * @author noahq
 * @version 1.3
 */
public class DefaultPatternStrategy implements PatternStrategy {

    @Override
    public void buildRole(PatternRole role, String className, CodeBuilder builder, GenerationContext ctx) {
        if (ctx.pedagogical()) {
            builder.withJavadoc("""
                    /**
                     * Pattern %s (%s)
                     * <p>%s</p>
                     * <p>Rôle : %s</p>
                     */""".formatted(
                    ctx.pattern().displayName(),
                    ctx.pattern().getCategory(),
                    ctx.pattern().getDescription(),
                    role.description()
            ));
        }

        // 1. Héritage / Implémentation
        if (role.parentId() != null) {
            ctx.pattern().getRole(role.parentId()).ifPresent(parentRole -> {
                String parentClass = ctx.primaryClassOf(role.parentId());
                String relation = (role.kind() != ClassKind.INTERFACE && parentRole.kind() == ClassKind.INTERFACE)
                        ? "implements" : "extends";
                builder.withParent(relation, parentClass);
            });
        }

        // 2. Dépendances (champs) et import éventuel d'une classe existante
        if (role.kind() != ClassKind.INTERFACE) {
            addDependencies(role, builder, ctx);
        }

        // 3. Méthodes (standard ou déléguées via MethodCollector)
        for (MethodCollector.ResolvedMethod method : MethodCollector.collect(role, ctx)) {
            if (method.signature().contains("List<")) {
                builder.addImport("java.util.List");
            }
            builder.addMethod(formatMethod(role, method, ctx.pedagogical()));
        }
    }

    private void addDependencies(PatternRole role, CodeBuilder builder, GenerationContext ctx) {
        ExistingClassInfo existing = ctx.existingClassInfo();
        PatternRole targetRole = ctx.existingTargetRole().orElse(null);

        for (String usedId : role.usesIds()) {
            String targetType = ctx.primaryClassOf(usedId);
            if (targetType != null && !targetType.isEmpty()) {
                builder.addField("private " + targetType + " " + MethodCollector.decapitalize(targetType) + ";");
                if (existing != null && targetRole != null && usedId.equals(targetRole.id())) {
                    addImportIfDifferentPackage(builder, existing, ctx.packageName());
                }
            }
        }

        // En mode WRAP (Facade), la classe existante est ajoutée directement comme champ du rôle cible
        if (existing != null && targetRole != null
                && targetRole.existingClassMode() == ExistingClassMode.WRAP
                && role.id().equals(targetRole.id())) {
            builder.addField("private " + existing.simpleName() + " " + MethodCollector.decapitalize(existing.simpleName()) + ";");
            addImportIfDifferentPackage(builder, existing, ctx.packageName());
        }
    }

    private void addImportIfDifferentPackage(CodeBuilder builder, ExistingClassInfo existing, String targetPackage) {
        if (!existing.packageName().isEmpty() && !existing.packageName().equals(targetPackage)) {
            builder.addImport(existing.qualifiedName());
        }
    }

    private String formatMethod(PatternRole role, MethodCollector.ResolvedMethod method, boolean pedagogical) {
        StringBuilder sb = new StringBuilder();
        if (method.isOverride()) {
            sb.append("    @Override\n");
        }
        if (role.kind() == ClassKind.INTERFACE) {
            return sb.append("    ").append(method.signature().replaceFirst("^abstract\\s+", "")).append(";").toString();
        }
        if (method.signature().startsWith("abstract ")) {
            return sb.append("    public ").append(method.signature()).append(";").toString();
        }

        sb.append("    public ").append(method.signature()).append(" {\n");
        if (method.customBody() != null) {
            sb.append(method.customBody()).append("\n");
        } else {
            if (pedagogical) {
                sb.append("        // TODO : Implémenter cette méthode\n");
            }
            sb.append(defaultReturnStatement(method.signature()));
        }
        return sb.append("    }").toString();
    }

    private String defaultReturnStatement(String signature) {
        String beforeParen = signature.substring(0, signature.indexOf('(')).trim();
        String[] tokens = beforeParen.split("\\s+");
        String returnType = tokens.length >= 2 ? tokens[tokens.length - 2] : "void";
        return switch (returnType) {
            case "void" -> "";
            case "boolean" -> "        return false;\n";
            case "int", "long", "short", "byte", "float", "double", "char" -> "        return 0;\n";
            default -> "        return null;\n";
        };
    }
}
