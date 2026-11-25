package com.noahquagh.quark.jcb.strategy;

import com.noahquagh.quark.jcb.GenerationContext;
import com.noahquagh.quark.jcb.JavaClassBuilder;
import com.noahquagh.quark.model.ClassKind;
import com.noahquagh.quark.model.DesignPatternType;
import com.noahquagh.quark.model.ExistingClassInfo;
import com.noahquagh.quark.model.PatternRole;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stratégie de génération générique pilotée par les métadonnées des {@link PatternRole}.
 * Gère l'héritage, les dépendances, la résolution des placeholders et l'implémentation des méthodes héritées.
 *
 * @author noahq
 * @version 1.3
 */
public class DefaultPatternStrategy implements PatternStrategy {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([A-Z0-9_]+)}");

    /**
     * Construit la structure complète d'un rôle standard (Javadoc, héritage, champs et méthodes).
     *
     * @param role      participant du design pattern à générer
     * @param className nom exact de la classe ou interface générée
     * @param builder   constructeur fluide du fichier source Java
     * @param ctx       contexte global de la génération
     */
    @Override
    public void buildRole(PatternRole role, String className, JavaClassBuilder builder, GenerationContext ctx) {

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

        if (role.parentId() != null) {
            ctx.pattern().getRole(role.parentId()).ifPresent(patternRole -> {
                String parentClass = ctx.primaryClassOf(role.parentId());
                String relation = (role.kind() != ClassKind.INTERFACE && patternRole.kind() == ClassKind.INTERFACE)
                        ? "implements" : "extends";
                builder.withParent(relation, parentClass);
            });
        }

        if (role.kind() != ClassKind.INTERFACE) {
            for (String usedId : role.usesIds()) {
                String targetType = ctx.primaryClassOf(usedId);
                if (targetType != null && !targetType.isEmpty()) {
                    String fieldName = decapitalize(targetType);
                    builder.addField("private " + targetType + " " + fieldName + ";");
                }
            }
        }

        for (MethodSpec method : collectMethods(role, ctx)) {
            if (method.signature().contains("List<")) {
                builder.addImport("java.util.List");
            }
            builder.addMethod(formatMethod(role, method, ctx.pedagogical()));
        }

        applyExistingClassIntegration(role, builder, ctx);
    }

    /**
     * Rassemble les méthodes propres au rôle ainsi que les méthodes abstraites ou d'interfaces
     * héritées de ses parents lorsque le rôle est une classe concrète.
     *
     * @param role rôle dont on collecte les méthodes
     * @param ctx  contexte global de la génération
     * @return liste ordonnée et dédupliquée des spécifications de méthodes
     */
    private List<MethodSpec> collectMethods(PatternRole role, GenerationContext ctx) {
        List<MethodSpec> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        Set<String> parentSignatures = new HashSet<>();
        List<String> inheritedAbstractOrInterfaceSigs = new ArrayList<>();

        if (role.parentId() != null) {
            String parentId = role.parentId();
            while (parentId != null) {
                Optional<PatternRole> parentOpt = ctx.pattern().getRole(parentId);
                if (parentOpt.isEmpty()) break;

                PatternRole parent = parentOpt.get();
                for (String rawParentSig : parent.methods()) {
                    String clean = rawParentSig.replaceFirst("^abstract\\s+", "");
                    for (String sig : resolvePlaceholders(clean, ctx)) {
                        parentSignatures.add(normalize(sig));
                        if (parent.kind() == ClassKind.INTERFACE || rawParentSig.startsWith("abstract ")) {
                            inheritedAbstractOrInterfaceSigs.add(sig);
                        }
                    }
                }
                parentId = parent.parentId();
            }
        }

        for (String raw : role.methods()) {
            for (String sig : resolvePlaceholders(raw, ctx)) {
                String norm = normalize(sig);
                boolean isOverride = role.parentId() != null
                        && role.kind() == ClassKind.CLASS
                        && !sig.startsWith("static ")
                        && !sig.startsWith("abstract ")
                        && parentSignatures.contains(norm);
                result.add(new MethodSpec(sig, isOverride));
                seen.add(norm);
            }
        }

        if (role.kind() == ClassKind.CLASS) {
            for (String sig : inheritedAbstractOrInterfaceSigs) {
                if (seen.add(normalize(sig))) {
                    result.add(new MethodSpec(sig, true));
                }
            }
        }

        return result;
    }

    /**
     * Remplace récursivement les placeholders {@code {Name}} et {@code {ROLE_ID}} dans une signature.
     * Si un rôle cible possède plusieurs instances (ex : Visitor), duplique la signature pour chacune.
     *
     * @param rawSig signature brute contenant d'éventuels placeholders
     * @param ctx    contexte global de la génération
     * @return liste des signatures résolues
     */
    private List<String> resolvePlaceholders(String rawSig, GenerationContext ctx) {
        String sig = rawSig.replace("{Name}", ctx.baseName());
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(sig);
        if (!matcher.find()) return List.of(sig);

        String roleId = matcher.group(1);
        List<String> targets = ctx.allClassesOf(roleId);
        if (targets.isEmpty()) return List.of(sig);

        List<String> expanded = new ArrayList<>();
        for (String target : targets) {
            expanded.addAll(resolvePlaceholders(sig.replace("{" + roleId + "}", target), ctx));
        }
        return expanded;
    }

    /**
     * Formate une méthode Java complète (signature, annotation {@code @Override} et corps par défaut).
     *
     * @param role        rôle propriétaire de la méthode
     * @param method      spécification de la méthode à formater
     * @param pedagogical {@code true} pour inclure un commentaire TODO explicatif
     * @return code source formaté de la méthode
     */
    private String formatMethod(PatternRole role, MethodSpec method, boolean pedagogical) {
        StringBuilder sb = new StringBuilder();
        if (method.isOverride()) {
            sb.append("    @Override\n");
        }
        if (role.kind() == ClassKind.INTERFACE) {
            sb.append("    ").append(method.signature().replaceFirst("^abstract\\s+", "")).append(";");
            return sb.toString();
        }
        if (method.signature().startsWith("abstract ")) {
            sb.append("    public ").append(method.signature()).append(";");
            return sb.toString();
        }

        sb.append("    public ").append(method.signature()).append(" {\n");
        if (pedagogical) {
            sb.append("        // TODO : Implémenter cette méthode\n");
        }
        sb.append(defaultReturnStatement(method.signature()));
        sb.append("    }");
        return sb.toString();
    }

    /**
     * Déduit une instruction {@code return} valide selon le type de retour de la signature
     * afin que le code généré compile immédiatement.
     *
     * @param signature signature complète de la méthode
     * @return instruction {@code return} adaptée, ou chaîne vide si le retour est {@code void}
     */
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

    /**
     * Normalise une signature en supprimant les modificateurs ({@code abstract}, {@code final}, {@code static})
     * pour détecter les doublons lors de l'héritage.
     *
     * @param sig signature à normaliser
     * @return signature nettoyée
     */
    private String normalize(String sig) {
        return sig.replaceFirst("^(abstract|final|static)\\s+", "").trim();
    }

    /**
     * Représente une méthode résolue avec son indicateur de redéfinition ({@code @Override}).
     *
     * @param signature  signature Java résolue
     * @param isOverride {@code true} si l'annotation {@code @Override} doit être ajoutée
     */
    private record MethodSpec(String signature, boolean isOverride) {}

    /**
     * Applique l'import et génère les méthodes déléguées réelles lorsqu'une classe existante
     * a été sélectionnée par l'utilisateur (Adapter, Decorator, Proxy, Facade).
     */
    protected void applyExistingClassIntegration(PatternRole role, JavaClassBuilder builder, GenerationContext ctx) {
        ExistingClassInfo existing = ctx.existingClassInfo();
        if (existing == null) return;

        boolean needsImport = !existing.packageName().isEmpty()
                && !existing.packageName().equals(ctx.packageName());

        if (needsImport && shouldImportExistingClass(ctx.pattern(), role.id())) {
            builder.addImport(existing.qualifiedName());
        }

        if (ctx.pattern() == DesignPatternType.FACADE && "FACADE".equals(role.id())) {
            String fieldName = decapitalize(existing.simpleName());
            builder.addField("private " + existing.simpleName() + " " + fieldName + ";");
        }

        if (isDelegationTargetRole(ctx.pattern(), role.id()) && !existing.methods().isEmpty()) {
            String delegateField = resolveDelegateFieldName(role, ctx, existing.simpleName());
            for (ExistingClassInfo.MethodSignature method : existing.methods()) {
                builder.addMethod(formatDelegatedMethod(role, method, delegateField, ctx.pedagogical()));
            }
        }
    }

    /**
     * Formate une méthode extraite d'une classe existante (signature d'interface ou implémentation déléguée).
     */
    private String formatDelegatedMethod(PatternRole role,
                                         ExistingClassInfo.MethodSignature method,
                                         String delegateField,
                                         boolean pedagogical) {
        if (role.kind() == ClassKind.INTERFACE) {
            return "    " + method.toDeclaration() + ";";
        }

        StringBuilder sb = new StringBuilder();
        if (role.parentId() != null) {
            sb.append("    @Override\n");
        }
        sb.append("    public ").append(method.toDeclaration()).append(" {\n");
        if (pedagogical) {
            sb.append("        // TODO : Ajouter un traitement spécifique avant ou après la délégation\n");
        }
        String call = delegateField + "." + method.name() + "(" + method.toCallArguments() + ");";
        if ("void".equals(method.returnType())) {
            sb.append("        ").append(call).append("\n");
        } else {
            sb.append("        return ").append(call).append("\n");
        }
        sb.append("    }");
        return sb.toString();
    }

    private boolean shouldImportExistingClass(DesignPatternType pattern, String roleId) {
        return switch (pattern) {
            case ADAPTER -> "ADAPTER".equals(roleId);
            case PROXY -> "PROXY".equals(roleId);
            case FACADE -> "FACADE".equals(roleId);
            case DECORATOR -> "CLIENT".equals(roleId);
            default -> false;
        };
    }

    private boolean isDelegationTargetRole(DesignPatternType pattern, String roleId) {
        return switch (pattern) {
            case PROXY -> Set.of("SUBJECT", "PROXY").contains(roleId);
            case DECORATOR -> Set.of("COMPONENT", "BASE_DECORATOR", "CONCRETE_DECORATOR").contains(roleId);
            case FACADE -> "FACADE".equals(roleId);
            default -> false;
        };
    }

    private String resolveDelegateFieldName(PatternRole role, GenerationContext ctx, String existingSimpleName) {
        if (ctx.pattern() == DesignPatternType.DECORATOR) {
            if ("CONCRETE_DECORATOR".equals(role.id())) {
                return "super";
            }
            return decapitalize(ctx.primaryClassOf("COMPONENT"));
        }
        return decapitalize(existingSimpleName);
    }

    private String decapitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}