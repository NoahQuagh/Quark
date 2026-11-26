package com.noahquagh.quark.generator;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiPackage;
import com.noahquagh.quark.jcb.JavaClassBuilder;
import com.noahquagh.quark.jcb.psi.PsiClassInspector;
import com.noahquagh.quark.jcb.psi.PsiClientInjector;
import com.noahquagh.quark.jcb.psi.PsiFileWriter;
import com.noahquagh.quark.jcb.strategy.DefaultPatternStrategy;
import com.noahquagh.quark.jcb.strategy.SingletonStrategy;
import com.noahquagh.quark.model.DesignPatternType;
import com.noahquagh.quark.model.ExistingClassMode;
import com.noahquagh.quark.model.PatternGenerationConfig;
import com.noahquagh.quark.model.PatternRole;

import java.util.*;

/**
 * Générateur des patrons de conception.
 * Résout les noms de participants, délègue la construction du code aux stratégies
 * et déclenche l'écriture dans le répertoire cible.
 *
 * @author noahq
 * @version 1.5
 */
public class PatternGenerator {

    private static final Map<DesignPatternType, PatternStrategy> CUSTOM_STRATEGIES = Map.of(
            DesignPatternType.SINGLETON, new SingletonStrategy()
    );

    private static final PatternStrategy DEFAULT_STRATEGY = new DefaultPatternStrategy();

    public static void generate(Project project, PsiDirectory directory, PatternGenerationConfig config) {
        generate(project, directory, config, null, null);
    }

    public static void generate(Project project,
                                PsiDirectory directory,
                                PatternGenerationConfig config,
                                PsiClass targetClientClass,
                                PsiClass targetExistingClass) {
        PsiDirectory targetDir = resolveTargetDirectory(project, directory, config);
        PsiPackage pkg = JavaDirectoryService.getInstance().getPackage(targetDir);
        String packageName = pkg != null ? pkg.getQualifiedName() : "";

        Map<String, String> files = generateSources(config, packageName);
        PsiFileWriter.writeFiles(project, targetDir, "Générer " + config.pattern().displayName(), files);

        if (config.existingClassInfo() != null && targetExistingClass != null) {
            config.pattern().findExistingTargetRole().ifPresent(targetRole -> {
                if (targetRole.existingClassMode() == ExistingClassMode.IMPLEMENT_PARENT && targetRole.parentId() != null) {
                    Map<String, List<String>> resolved = resolveAllClassNames(config);
                    List<String> parents = resolved.get(targetRole.parentId());
                    if (parents != null && !parents.isEmpty()) {
                        String interfaceFqn = qualify(packageName, parents.get(0));
                        List<String> missingMethods = config.existingClassInfo().methods().isEmpty()
                                ? config.pattern().getRole(targetRole.parentId())
                                .map(parent -> parent.methods().stream()
                                        .map(m -> m.replace("{Name}", config.baseName()))
                                        .toList())
                                .orElse(List.of())
                                : List.of();

                        WriteCommandAction.runWriteCommandAction(project, "Implémenter " + parents.get(0), null,
                                () -> PsiClassInspector.makeClassImplementInterface(project, targetExistingClass, interfaceFqn, missingMethods));
                    }
                }
            });
        }

        if (config.injectClientIntoActiveClass() && targetClientClass != null) {
            String qualifiedType = resolveClientTargetQualifiedType(config, packageName);
            if (qualifiedType != null) {
                PsiClientInjector.injectClientField(project, targetClientClass, qualifiedType);
            }
        }
    }

    public static Map<String, String> generateSources(DesignPatternType pattern, String baseName,
                                                      String packageName, boolean pedagogical) {
        Set<String> allOptionals = pattern.getRoles().stream()
                .filter(r -> !r.required())
                .map(PatternRole::id)
                .collect(java.util.stream.Collectors.toSet());

        return generateSources(
                new PatternGenerationConfig(pattern, baseName, false, pedagogical, allOptionals, Map.of(), List.of()),
                packageName
        );
    }

    public static Map<String, String> generateSources(PatternGenerationConfig config, String packageName) {
        DesignPatternType pattern = config.pattern();
        Map<String, List<String>> roleClasses = resolveAllClassNames(config);
        GenerationContext ctx = new GenerationContext(
                pattern, config.baseName(), packageName, roleClasses,
                config.enabledOptionalRoleIds(), config.roleInstanceCounts(),
                config.variants(), config.pedagogical(), config.existingClassInfo()
        );
        PatternStrategy strategy = CUSTOM_STRATEGIES.getOrDefault(pattern, DEFAULT_STRATEGY);

        String skippedReplacedRoleId = (config.existingClassInfo() != null)
                ? pattern.findExistingTargetRole()
                .filter(r -> r.existingClassMode() != ExistingClassMode.WRAP)
                .map(PatternRole::id)
                .orElse(null)
                : null;

        Map<String, String> files = new LinkedHashMap<>();
        for (PatternRole role : pattern.getRoles()) {
            if (role.id().equals(skippedReplacedRoleId)) {
                continue;
            }
            for (String className : ctx.allClassesOf(role.id())) {
                JavaClassBuilder builder = new JavaClassBuilder(packageName, role.kind(), className);
                strategy.buildRole(role, className, builder, ctx);
                files.put(className + ".java", builder.build());
            }
        }
        return files;
    }

    private static Map<String, List<String>> resolveAllClassNames(PatternGenerationConfig config) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        List<String> variants = config.variants() != null ? config.variants() : List.of();
        String clientRoleId = config.pattern().findClientRole().map(PatternRole::id).orElse(null);
        PatternRole existingTarget = (config.existingClassInfo() != null)
                ? config.pattern().findExistingTargetRole().orElse(null)
                : null;

        for (PatternRole role : config.pattern().getRoles()) {
            if (role.id().equals(clientRoleId) && config.injectClientIntoActiveClass()) {
                continue;
            }
            if (!role.required() && !config.enabledOptionalRoleIds().contains(role.id())) {
                continue;
            }
            if (existingTarget != null && role.id().equals(existingTarget.id())
                    && existingTarget.existingClassMode() != ExistingClassMode.WRAP) {
                map.put(role.id(), List.of(config.existingClassInfo().simpleName()));
                continue;
            }

            if (role.multiple()) {
                int count = Math.max(1, config.roleInstanceCounts().getOrDefault(role.id(), role.defaultInstances()));
                List<String> names = new ArrayList<>();
                for (int i = 1; i <= count; i++) {
                    names.add(resolveRoleNameWithVariants(role, config.baseName(), i, variants));
                }
                map.put(role.id(), names);
            } else {
                map.put(role.id(), List.of(role.resolveName(config.baseName(), 1)));
            }
        }
        return map;
    }

    private static PsiDirectory resolveTargetDirectory(Project project, PsiDirectory directory, PatternGenerationConfig config) {
        if (!config.createSubPackage()) return directory;
        String subPkg = config.pattern().getSubPackage();
        PsiDirectory existing = directory.findSubdirectory(subPkg);
        if (existing != null) return existing;

        PsiDirectory[] created = new PsiDirectory[1];
        WriteCommandAction.runWriteCommandAction(project, "Créer le sous-package " + subPkg, null,
                () -> created[0] = directory.createSubdirectory(subPkg));
        return created[0] != null ? created[0] : directory;
    }

    private static String resolveClientTargetQualifiedType(PatternGenerationConfig config, String packageName) {
        PatternRole clientRole = config.pattern().findClientRole().orElse(null);
        if (clientRole == null || clientRole.usesIds().isEmpty()) return null;

        String targetRoleId = clientRole.usesIds().get(0);
        List<String> targetClasses = resolveAllClassNames(config).get(targetRoleId);
        if (targetClasses == null || targetClasses.isEmpty()) return null;

        return qualify(packageName, targetClasses.get(0));
    }

    private static String qualify(String packageName, String simpleName) {
        return packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
    }

    private static String resolveRoleNameWithVariants(PatternRole role, String baseName, int index, List<String> variants) {
        if (!variants.isEmpty() && index <= variants.size()) {
            String clean = variants.get(index - 1).replaceAll("[^a-zA-Z0-9_]", "");
            if (!clean.isEmpty()) {
                String variant = Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
                return role.namePattern()
                        .replace("{Name}", baseName)
                        .replace("{Index}", variant)
                        .replace("{Letter}", variant);
            }
        }
        return role.resolveName(baseName, index);
    }
}
