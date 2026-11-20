package com.github.noahquagh.quark.generator;

import com.github.noahquagh.quark.model.ClassKind;
import com.github.noahquagh.quark.model.DesignPatternType;
import com.github.noahquagh.quark.model.PatternRole;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.codeStyle.CodeStyleManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author noahq
 * @version 1.2
 */
public class PatternGenerator {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([A-Z0-9_]+)}");

    public static void generate(Project project,
                                PsiDirectory directory,
                                DesignPatternType pattern,
                                String baseName,
                                boolean pedagogical) {

        PsiPackage pkg = JavaDirectoryService.getInstance().getPackage(directory);
        String packageName = (pkg != null) ? pkg.getQualifiedName() : "";
        String packageHeader = packageName.isEmpty() ? "" : "package " + packageName + ";\n\n";

        // résoudre tous les noms de classes par rôle (ex: "STRATEGY" -> ["PaymentStrategy"])
        Map<String, List<String>> roleClasses = resolveAllClassNames(pattern, baseName);

        // générer le code source de chaque fichier
        Map<String, String> filesToCreate = new LinkedHashMap<>();
        for (PatternRole role : pattern.getRoles()) {
            List<String> classNames = roleClasses.get(role.id());
            for (String className : classNames) {
                String sourceCode = buildClassSource(pattern, role, className, baseName, packageHeader, roleClasses, pedagogical);
                filesToCreate.put(className + ".java", sourceCode);
            }
        }

        // ecrire les fichiers dans le PSI IntelliJ
        WriteCommandAction.runWriteCommandAction(project, "Générer " + pattern.displayName(), null, () -> {
            PsiFile firstCreated = null;

            for (Map.Entry<String, String> entry : filesToCreate.entrySet()) {
                String fileName = entry.getKey();
                String code = entry.getValue();

                if (directory.findFile(fileName) != null) {
                    Messages.showWarningDialog(project,
                            "Le fichier " + fileName + " existe déjà et a été ignoré.",
                            "Fichier existant");
                    continue;
                }

                PsiFile psiFile = PsiFileFactory.getInstance(project)
                        .createFileFromText(fileName, JavaFileType.INSTANCE, code);
                CodeStyleManager.getInstance(project).reformat(psiFile);

                PsiFile added = (PsiFile) directory.add(psiFile);
                if (firstCreated == null) {
                    firstCreated = added;
                }
            }

            if (firstCreated != null && firstCreated.getVirtualFile() != null) {
                FileEditorManager.getInstance(project).openFile(firstCreated.getVirtualFile(), true);
            }
        });
    }

    private static Map<String, List<String>> resolveAllClassNames(DesignPatternType pattern, String baseName) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (PatternRole role : pattern.getRoles()) {
            int count = Math.max(1, role.defaultInstances());
            List<String> names = new ArrayList<>();
            for (int i = 1; i <= count; i++) {
                names.add(role.resolveName(baseName, i));
            }
            map.put(role.id(), names);
        }
        return map;
    }

    private static String buildClassSource(DesignPatternType pattern,
                                           PatternRole role,
                                           String className,
                                           String baseName,
                                           String packageHeader,
                                           Map<String, List<String>> roleClasses,
                                           boolean pedagogical) {

        List<MethodSpec> methods = collectMethods(pattern, role, baseName, roleClasses);
        boolean needsListImport = methods.stream().anyMatch(m -> m.signature.contains("List<"));

        StringBuilder sb = new StringBuilder();
        sb.append(packageHeader);
        if (needsListImport) {
            sb.append("import java.util.List;\n\n");
        }

        // Javadoc pédagogique
        if (pedagogical) {
            sb.append("/**\n");
            sb.append(" * Pattern ").append(pattern.displayName()).append(" (").append(pattern.getCategory()).append(")\n");
            sb.append(" * <p>").append(pattern.getDescription()).append("</p>\n");
            sb.append(" * <p>Rôle : ").append(role.description()).append("</p>\n");
            sb.append(" */\n");
        }

        // En-tête de classe / interface
        sb.append("public ").append(role.kind().getKeyword()).append(" ").append(className);
        if (role.parentId() != null) {
            Optional<PatternRole> parentOpt = pattern.getRole(role.parentId());
            List<String> parentNames = roleClasses.get(role.parentId());
            if (parentOpt.isPresent() && parentNames != null && !parentNames.isEmpty()) {
                PatternRole parentRole = parentOpt.get();
                String relation = (role.kind() != ClassKind.INTERFACE && parentRole.kind() == ClassKind.INTERFACE)
                        ? " implements "
                        : " extends ";
                sb.append(relation).append(parentNames.get(0));
            }
        }
        sb.append(" {\n\n");

        // Cas particulier du Singleton : champ static instance + constructeur privé
        if (pattern == DesignPatternType.SINGLETON && role.id().equals("SINGLETON")) {
            sb.append("    private static ").append(className).append(" instance;\n\n");
            sb.append("    private ").append(className).append("() {}\n\n");
        }

        // Champs issus de usesIds (sauf dans les interfaces)
        if (role.kind() != ClassKind.INTERFACE && !role.usesIds().isEmpty()) {
            for (String usedId : role.usesIds()) {
                List<String> targetNames = roleClasses.get(usedId);
                if (targetNames != null && !targetNames.isEmpty()) {
                    String targetType = targetNames.get(0);
                    String fieldName = uncapitalize(targetType);
                    sb.append("    private ").append(targetType).append(" ").append(fieldName).append(";\n");
                }
            }
            sb.append("\n");
        }

        // Géné des méthodes
        for (MethodSpec method : methods) {
            if (method.isOverride) {
                sb.append("    @Override\n");
            }

            if (role.kind() == ClassKind.INTERFACE) {
                // Dans une interface retire "abstract " s'il présent
                String cleanSig = method.signature.replaceFirst("^abstract\\s+", "");
                sb.append("    ").append(cleanSig).append(";\n\n");
            } else if (method.signature.startsWith("abstract ")) {
                sb.append("    public ").append(method.signature).append(";\n\n");
            } else {
                sb.append("    public ").append(method.signature).append(" {\n");
                sb.append(buildDefaultBody(pattern, className, method.signature, pedagogical));
                sb.append("    }\n\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Collecte les méthodes propres au rôle + les méthodes abstraites/d'interface héritées du parent.
     */
    private static List<MethodSpec> collectMethods(DesignPatternType pattern,
                                                   PatternRole role,
                                                   String baseName,
                                                   Map<String, List<String>> roleClasses) {
        List<MethodSpec> result = new ArrayList<>();
        Set<String> declaredSignatures = new LinkedHashSet<>();

        // méthodes déclarées directement sur le rôle
        for (String rawSig : role.methods()) {
            for (String resolved : resolveMethodPlaceholders(rawSig, baseName, roleClasses)) {
                boolean isOverride = role.parentId() != null && role.kind() == ClassKind.CLASS
                        && !resolved.startsWith("static ") && !resolved.startsWith("abstract ");
                result.add(new MethodSpec(resolved, isOverride));
                declaredSignatures.add(normalizeSignature(resolved));
            }
        }

        // si c'est une classe concrète avec un parent, on hérite des méthodes à implémenter
        if (role.kind() == ClassKind.CLASS && role.parentId() != null) {
            String currentParentId = role.parentId();
            while (currentParentId != null) {
                Optional<PatternRole> parentOpt = pattern.getRole(currentParentId);
                if (parentOpt.isEmpty()) break;

                PatternRole parent = parentOpt.get();
                for (String rawParentSig : parent.methods()) {
                    boolean mustImplement = parent.kind() == ClassKind.INTERFACE
                            || rawParentSig.startsWith("abstract ");
                    if (!mustImplement) continue;

                    String concreteSig = rawParentSig.replaceFirst("^abstract\\s+", "");
                    for (String resolved : resolveMethodPlaceholders(concreteSig, baseName, roleClasses)) {
                        if (declaredSignatures.add(normalizeSignature(resolved))) {
                            result.add(new MethodSpec(resolved, true));
                        }
                    }
                }
                currentParentId = parent.parentId();
            }
        }

        return result;
    }

    /**
     * Remplace {Name} et {ROLE_ID}. Si {ROLE_ID} cible un rôle multiple (ex: Visitor),
     * duplique la signature pour chaque instance.
     */
    private static List<String> resolveMethodPlaceholders(String rawSig,
                                                          String baseName,
                                                          Map<String, List<String>> roleClasses) {
        String sig = rawSig.replace("{Name}", baseName);
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(sig);

        if (!matcher.find()) {
            return List.of(sig);
        }

        String roleId = matcher.group(1);
        List<String> targets = roleClasses.get(roleId);
        if (targets == null || targets.isEmpty()) {
            return List.of(sig);
        }

        List<String> expanded = new ArrayList<>();
        for (String targetClass : targets) {
            String replaced = sig.replace("{" + roleId + "}", targetClass);
            // evite les doublons de noms de paramètres (ex: visit(ConcreteElementA element))
            expanded.addAll(resolveMethodPlaceholders(replaced, baseName, roleClasses));
        }
        return expanded;
    }

    private static String buildDefaultBody(DesignPatternType pattern,
                                           String className,
                                           String signature,
                                           boolean pedagogical) {
        StringBuilder body = new StringBuilder();
        if (pedagogical) {
            body.append("        // TODO : Implémenter cette méthode\n");
        }

        // cas spécifique Singleton getInstance()
        if (pattern == DesignPatternType.SINGLETON && signature.contains("getInstance()")) {
            body.append("        if (instance == null) {\n");
            body.append("            instance = new ").append(className).append("();\n");
            body.append("        }\n");
            body.append("        return instance;\n");
            return body.toString();
        }

        // recup du type de retour (avant le nom de la méthode)
        String beforeParen = signature.substring(0, signature.indexOf('(')).trim();
        String[] tokens = beforeParen.split("\\s+");
        String returnType = tokens.length >= 2 ? tokens[tokens.length - 2] : "void";

        switch (returnType) {
            case "void" -> { /* Pas de return */ }
            case "boolean" -> body.append("        return false;\n");
            case "int", "long", "short", "byte", "float", "double", "char" -> body.append("        return 0;\n");
            default -> body.append("        return null;\n");
        }
        return body.toString();
    }

    private static String normalizeSignature(String sig) {
        return sig.replaceFirst("^(abstract|final|static)\\s+", "").trim();
    }

    private static String uncapitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    private record MethodSpec(String signature, boolean isOverride) {}
}
