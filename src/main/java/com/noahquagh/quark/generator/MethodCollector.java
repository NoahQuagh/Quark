package com.noahquagh.quark.generator;

import com.noahquagh.quark.model.ClassKind;
import com.noahquagh.quark.model.ExistingClassInfo;
import com.noahquagh.quark.model.ExistingClassMode;
import com.noahquagh.quark.model.PatternRole;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Résout les signatures et les corps de méthodes d'un {@link PatternRole}
 * (héritage, placeholders et délégation vers une classe existante).
 *
 * @author noahq
 * @version 1.1
 */
public class MethodCollector {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([A-Z0-9_]+)}");

    public record ResolvedMethod(String signature, boolean isOverride, String customBody) {
        ResolvedMethod(String signature, boolean isOverride) {
            this(signature, isOverride, null);
        }
    }

    public static List<ResolvedMethod> collect(PatternRole role, GenerationContext ctx) {
        Optional<PatternRole> existingTarget = ctx.existingTargetRole();
        if (existingTarget.isPresent()) {
            ExistingClassInfo effectiveInfo = resolveEffectiveExistingInfo(existingTarget.get(), ctx);
            if (!effectiveInfo.methods().isEmpty()) {
                List<ResolvedMethod> delegated = collectFromExistingClass(role, existingTarget.get(), effectiveInfo, ctx);
                if (!delegated.isEmpty()) {
                    return delegated;
                }
            }
        }
        return collectStandardMethods(role, ctx);
    }

    private static ExistingClassInfo resolveEffectiveExistingInfo(PatternRole targetRole, GenerationContext ctx) {
        ExistingClassInfo info = ctx.existingClassInfo();
        if (!info.methods().isEmpty() || targetRole.existingClassMode() != ExistingClassMode.IMPLEMENT_PARENT || targetRole.parentId() == null) {
            return info;
        }
        List<ExistingClassInfo.MethodSignature> fallback = ctx.pattern().getRole(targetRole.parentId())
                .map(parent -> parent.methods().stream()
                        .map(raw -> parseSignature(raw.replace("{Name}", ctx.baseName())))
                        .filter(Objects::nonNull)
                        .toList())
                .orElse(List.of());

        return new ExistingClassInfo(info.qualifiedName(), info.simpleName(), info.packageName(), info.isInterface(), fallback);
    }

    private static ExistingClassInfo.MethodSignature parseSignature(String raw) {
        String clean = raw.replaceFirst("^abstract\\s+", "").trim();
        int open = clean.indexOf('(');
        int close = clean.lastIndexOf(')');
        if (open <= 0 || close < open) return null;

        String[] header = clean.substring(0, open).trim().split("\\s+");
        if (header.length < 2) return null;

        String returnType = header[header.length - 2];
        String name = header[header.length - 1];
        String paramsPart = clean.substring(open + 1, close).trim();
        List<String> params = paramsPart.isEmpty() ? List.of() : Arrays.stream(paramsPart.split(",")).map(String::trim).toList();
        return new ExistingClassInfo.MethodSignature(returnType, name, params);
    }

    private static List<ResolvedMethod> collectFromExistingClass(PatternRole role,
                                                                 PatternRole targetRole,
                                                                 ExistingClassInfo existing,
                                                                 GenerationContext ctx) {
        ExistingClassMode mode = targetRole.existingClassMode();

        if (mode == ExistingClassMode.WRAP && role.id().equals(targetRole.id())) {
            String fieldName = decapitalize(existing.simpleName());
            return existing.methods().stream()
                    .map(m -> new ResolvedMethod(m.toDeclaration(), false, buildDelegationBody(m, fieldName, ctx.pedagogical())))
                    .toList();
        }

        String parentInterfaceId = targetRole.parentId();
        if (mode == ExistingClassMode.IMPLEMENT_PARENT && parentInterfaceId != null && isPartOfHierarchy(role, parentInterfaceId, ctx)) {
            boolean isOverride = role.kind() != ClassKind.INTERFACE && role.parentId() != null;
            String delegateTarget = resolveHierarchyDelegate(role, parentInterfaceId, ctx);

            return existing.methods().stream()
                    .map(m -> new ResolvedMethod(
                            m.toDeclaration(),
                            isOverride,
                            role.kind() == ClassKind.INTERFACE ? null : buildDelegationBody(m, delegateTarget, ctx.pedagogical())
                    ))
                    .toList();
        }

        return List.of();
    }

    private static boolean isPartOfHierarchy(PatternRole role, String rootInterfaceId, GenerationContext ctx) {
        if (role.id().equals(rootInterfaceId)) return true;
        String currentParent = role.parentId();
        while (currentParent != null) {
            if (currentParent.equals(rootInterfaceId)) return true;
            currentParent = ctx.pattern().getRole(currentParent).map(PatternRole::parentId).orElse(null);
        }
        return false;
    }

    private static String resolveHierarchyDelegate(PatternRole role, String interfaceId, GenerationContext ctx) {
        if (role.parentId() != null && !role.parentId().equals(interfaceId)) {
            return "super";
        }
        String interfaceName = ctx.primaryClassOf(interfaceId);
        return interfaceName != null ? decapitalize(interfaceName) : "delegate";
    }

    private static String buildDelegationBody(ExistingClassInfo.MethodSignature method, String target, boolean pedagogical) {
        StringBuilder body = new StringBuilder();
        if (pedagogical) {
            body.append("        // TODO : Ajouter un traitement spécifique avant ou après la délégation\n");
        }
        String call = target + "." + method.name() + "(" + method.toCallArguments() + ");";
        body.append("void".equals(method.returnType()) ? "        " + call : "        return " + call);
        return body.toString();
    }

    private static List<ResolvedMethod> collectStandardMethods(PatternRole role, GenerationContext ctx) {
        List<ResolvedMethod> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        Set<String> parentSignatures = new HashSet<>();
        List<String> inheritedAbstractSigs = new ArrayList<>();

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
                        inheritedAbstractSigs.add(sig);
                    }
                }
            }
            parentId = parent.parentId();
        }

        for (String raw : role.methods()) {
            for (String sig : resolvePlaceholders(raw, ctx)) {
                String norm = normalize(sig);
                boolean isOverride = role.parentId() != null && role.kind() == ClassKind.CLASS
                        && !sig.startsWith("static ") && !sig.startsWith("abstract ")
                        && parentSignatures.contains(norm);
                result.add(new ResolvedMethod(sig, isOverride));
                seen.add(norm);
            }
        }

        if (role.kind() == ClassKind.CLASS) {
            for (String sig : inheritedAbstractSigs) {
                if (seen.add(normalize(sig))) {
                    result.add(new ResolvedMethod(sig, true));
                }
            }
        }
        return result;
    }

    private static List<String> resolvePlaceholders(String rawSig, GenerationContext ctx) {
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

    private static String normalize(String sig) {
        return sig.replaceFirst("^(abstract|final|static)\\s+", "").trim();
    }

    public static String decapitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}