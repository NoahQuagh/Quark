package com.noahquagh.quark.generator.strategy;

import com.noahquagh.quark.generator.GenerationContext;
import com.noahquagh.quark.generator.JavaClassBuilder;
import com.noahquagh.quark.model.ClassKind;
import com.noahquagh.quark.model.PatternRole;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stratégie de génération générique pilotée par les métadonnées des {@link PatternRole}.
 * Gère l'héritage, les dépendances, la résolution des placeholders et l'implémentation des méthodes héritées.
 *
 * @author noahq
 * @version 1.0
 */
public class DefaultPatternStrategy implements PatternStrategy{

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

        //javadoc si pedagog select
        if (ctx.pedagogical()){
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

        //heritage
        if(role.parentId() != null){
            ctx.pattern().getRole(role.parentId()).ifPresent(patternRole -> {
                String parentClass = ctx.primaryClassOf(role.parentId());
                String relation = (role.kind() != ClassKind.INTERFACE && patternRole.kind() == ClassKind.INTERFACE)
                    ? "implements" : "extends";
                builder.withParent(relation,parentClass);
            });
        }

        //dependances
        if (role.kind() != ClassKind.INTERFACE) {
            for (String usedId : role.usesIds()) {
                String targetType = ctx.primaryClassOf(usedId);
                if (targetType != null) {
                    String fieldName = Character.toLowerCase(targetType.charAt(0)) + targetType.substring(1);
                    builder.addField("private " + targetType + " " + fieldName + ";");
                }
            }
        }

        //collecte et generation des methodes
        for (MethodSpec method : collectMethods(role, ctx)) {
            if (method.signature().contains("List<")) {
                builder.addImport("java.util.List");
            }
            builder.addMethod(formatMethod(role, method, ctx.pedagogical()));
        }
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

        for (String raw : role.methods()) {
            for (String sig : resolvePlaceholders(raw, ctx)) {
                boolean isOverride = role.parentId() != null && role.kind() == ClassKind.CLASS
                        && !sig.startsWith("static ") && !sig.startsWith("abstract ");
                result.add(new MethodSpec(sig, isOverride));
                seen.add(normalize(sig));
            }
        }

        //heritage des methodes abs et interface pour les classes concrete
        if (role.kind() == ClassKind.CLASS && role.parentId() != null) {
            String parentId = role.parentId();
            while (parentId != null) {
                Optional<PatternRole> parentOpt = ctx.pattern().getRole(parentId);
                if (parentOpt.isEmpty()) break;

                PatternRole parent = parentOpt.get();
                for (String rawParentSig : parent.methods()) {
                    if (parent.kind() == ClassKind.INTERFACE || rawParentSig.startsWith("abstract ")) {
                        String clean = rawParentSig.replaceFirst("^abstract\\s+", "");
                        for (String sig : resolvePlaceholders(clean, ctx)) {
                            if (seen.add(normalize(sig))) {
                                result.add(new MethodSpec(sig, true));
                            }
                        }
                    }
                }
                parentId = parent.parentId();
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
    private record MethodSpec(String signature,boolean isOverride){}
}
