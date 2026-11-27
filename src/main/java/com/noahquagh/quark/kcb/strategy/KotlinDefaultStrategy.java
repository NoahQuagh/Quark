package com.noahquagh.quark.kcb.strategy;

import com.noahquagh.quark.generator.CodeBuilder;
import com.noahquagh.quark.generator.GenerationContext;
import com.noahquagh.quark.generator.MethodCollector;
import com.noahquagh.quark.generator.PatternStrategy;
import com.noahquagh.quark.kcb.KotlinClassBuilder;
import com.noahquagh.quark.model.ClassKind;
import com.noahquagh.quark.model.ExistingClassInfo;
import com.noahquagh.quark.model.ExistingClassMode;
import com.noahquagh.quark.model.PatternRole;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Stratégie de génération générique pour Kotlin pilotée par les métadonnées des {@link PatternRole}.
 *
 * @author noahq
 * @version 1.0
 */
public class KotlinDefaultStrategy implements PatternStrategy {

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

        if (builder instanceof KotlinClassBuilder ktBuilder && isInheritedByAnotherRole(role, ctx)) {
            ktBuilder.asOpen();
        }

        if (role.parentId() != null) {
            ctx.pattern().getRole(role.parentId()).ifPresent(parentRole -> {
                String parentClass = ctx.primaryClassOf(role.parentId());
                String relation = (role.kind() != ClassKind.INTERFACE && parentRole.kind() == ClassKind.INTERFACE)
                        ? "implements" : "extends";
                builder.withParent(relation, parentClass);
            });
        }

        if (role.kind() != ClassKind.INTERFACE) {
            addDependencies(role, builder, ctx);
        }

        for (MethodCollector.ResolvedMethod method : MethodCollector.collect(role, ctx)) {
            builder.addMethod(formatKotlinMethod(role, method, ctx.pedagogical()));
        }
    }

    private boolean isInheritedByAnotherRole(PatternRole role, GenerationContext ctx) {
        return role.kind() == ClassKind.CLASS && ctx.pattern().getRoles().stream()
                .anyMatch(other -> role.id().equals(other.parentId()));
    }

    private void addDependencies(PatternRole role, CodeBuilder builder, GenerationContext ctx) {
        ExistingClassInfo existing = ctx.existingClassInfo();
        PatternRole targetRole = ctx.existingTargetRole().orElse(null);

        for (String usedId : role.usesIds()) {
            String targetType = ctx.primaryClassOf(usedId);
            if (targetType != null && !targetType.isEmpty()) {
                String fieldName = MethodCollector.decapitalize(targetType);
                builder.addField("private var " + fieldName + ": " + targetType + "? = null");
                if (existing != null && targetRole != null && usedId.equals(targetRole.id())) {
                    addImportIfDifferentPackage(builder, existing, ctx.packageName());
                }
            }
        }

        if (existing != null && targetRole != null
                && targetRole.existingClassMode() == ExistingClassMode.WRAP
                && role.id().equals(targetRole.id())) {
            String fieldName = MethodCollector.decapitalize(existing.simpleName());
            builder.addField("private var " + fieldName + ": " + existing.simpleName() + "? = null");
            addImportIfDifferentPackage(builder, existing, ctx.packageName());
        }
    }

    private void addImportIfDifferentPackage(CodeBuilder builder, ExistingClassInfo existing, String targetPackage) {
        if (!existing.packageName().isEmpty() && !existing.packageName().equals(targetPackage)) {
            builder.addImport(existing.qualifiedName());
        }
    }

    private String formatKotlinMethod(PatternRole role, MethodCollector.ResolvedMethod method, boolean pedagogical) {
        String rawSig = method.signature();
        boolean isAbstract = rawSig.startsWith("abstract ");
        boolean isStatic = rawSig.startsWith("static ");
        String cleanSig = rawSig.replaceFirst("^(abstract|final|static)\\s+", "").trim();

        int open = cleanSig.indexOf('(');
        int close = cleanSig.lastIndexOf(')');
        String[] headerTokens = cleanSig.substring(0, open).trim().split("\\s+");
        String javaReturnType = headerTokens[headerTokens.length - 2];
        String methodName = headerTokens[headerTokens.length - 1];
        String rawParams = cleanSig.substring(open + 1, close).trim();

        String ktParams = convertParameters(rawParams);
        String ktReturn = convertType(javaReturnType);
        String returnSuffix = "Unit".equals(ktReturn) ? "" : ": " + ktReturn;

        StringBuilder sb = new StringBuilder();
        if (isStatic) {
            sb.append("    companion object {\n    ");
        }

        String prefix = "    ";
        if (method.isOverride()) {
            prefix += "override ";
        } else if (role.kind() == ClassKind.ABSTRACT_CLASS && !isAbstract && !isStatic) {
            prefix += "open ";
        }
        if (isAbstract && role.kind() != ClassKind.INTERFACE) {
            prefix += "abstract ";
        }

        sb.append(prefix).append("fun ").append(methodName).append("(").append(ktParams).append(")").append(returnSuffix);

        if (role.kind() == ClassKind.INTERFACE || isAbstract) {
            if (isStatic) sb.append("\n    }");
            return sb.toString();
        }

        sb.append(" {\n");
        if (method.customBody() != null) {
            String ktBody = method.customBody()
                    .replace(";", "")
                    .replaceFirst("(\\w+)\\.(\\w+\\()", "$1?.$2");
            sb.append(ktBody).append("\n");
        } else {
            if (pedagogical) {
                sb.append("        // TODO : Implémenter cette méthode\n");
            }
            sb.append(defaultKotlinReturnStatement(ktReturn));
        }
        sb.append("    }");

        if (isStatic) {
            sb.append("\n    }");
        }
        return sb.toString();
    }

    private String convertParameters(String rawParams) {
        if (rawParams.isEmpty()) return "";
        return Arrays.stream(rawParams.split(","))
                .map(String::trim)
                .map(param -> {
                    String[] parts = param.split("\\s+");
                    if (parts.length < 2) return param;
                    String type = convertType(parts[0]);
                    String name = parts[1];
                    return name + ": " + type;
                })
                .collect(Collectors.joining(", "));
    }

    private String convertType(String javaType) {
        return switch (javaType) {
            case "void" -> "Unit";
            case "boolean" -> "Boolean";
            case "int" -> "Int";
            case "long" -> "Long";
            case "double" -> "Double";
            case "float" -> "Float";
            case "Object" -> "Any";
            default -> javaType;
        };
    }

    private String defaultKotlinReturnStatement(String ktReturnType) {
        return switch (ktReturnType) {
            case "Unit" -> "";
            case "Boolean" -> "        return false\n";
            case "Int", "Long" -> "        return 0\n";
            case "Double", "Float" -> "        return 0.0\n";
            default -> "        return TODO(\"Retourner une instance de " + ktReturnType + "\")\n";
        };
    }
}