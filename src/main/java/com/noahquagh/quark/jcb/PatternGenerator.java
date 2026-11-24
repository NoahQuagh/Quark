package com.noahquagh.quark.jcb;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiPackage;
import com.noahquagh.quark.jcb.strategy.DefaultPatternStrategy;
import com.noahquagh.quark.jcb.strategy.PatternStrategy;
import com.noahquagh.quark.jcb.strategy.SingletonStrategy;
import com.noahquagh.quark.model.DesignPatternType;
import com.noahquagh.quark.model.PatternGenerationConfig;
import com.noahquagh.quark.model.PatternRole;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Générateur des patrons de conception.
 * Résout les noms de participants, délègue la construction du code aux stratégies
 * et déclenche l'écriture dans le répertoire cible.
 *
 * @author noahq
 * @version 1.2
 */
public class PatternGenerator {

    private static final Map<DesignPatternType, PatternStrategy> CUSTOM_STRATEGIES = Map.of(
            DesignPatternType.SINGLETON, new SingletonStrategy()
    );

    private static final PatternStrategy DEFAULT_STRATEGY = new DefaultPatternStrategy();

    /**
     * Génère et écrit physiquement les fichiers Java d'un patron de conception dans un répertoire IntelliJ.
     *
     * @param project     projet IntelliJ actif
     * @param directory   répertoire cible correspondant au package sélectionné
     * @param pattern     patron de conception à générer
     * @param baseName    nom de base saisi par l'utilisateur
     * @param pedagogical {@code true} pour inclure les commentaires pédagogiques
     */
    public static void generate(Project project,
                                PsiDirectory directory,
                                DesignPatternType pattern,
                                String baseName,
                                boolean pedagogical) {
        PsiPackage pkg = JavaDirectoryService.getInstance().getPackage(directory);
        String packageName = (pkg != null) ? pkg.getQualifiedName() : "";

        Map<String, String> files = generateSources(pattern, baseName, packageName, pedagogical);
        PsiFileWriter.writeFiles(project, directory, "Générer " + pattern.displayName(), files);
    }

    /**
     * Génère et écrit physiquement les fichiers Java d'un patron selon la configuration complète
     * issue de la boîte de dialogue (sous-package, rôles optionnels, multiplicité et variantes).
     *
     * @param project
     * @param directory
     * @param patternConfig
     */
    public static void generate(Project project, PsiDirectory directory, PatternGenerationConfig patternConfig){
        PsiDirectory targetDir = directory;
        if (patternConfig.createSubPackage()) {
                        String subPkg = patternConfig.pattern().getSubPackage();
            PsiDirectory existing = directory.findSubdirectory(subPkg);
            if (existing != null) {
                targetDir = existing;
            } else {
                PsiDirectory[] created = new PsiDirectory[1];
                com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(
                        project, "Créer le sous-package " + subPkg, null,
                        () -> created[0] = directory.createSubdirectory(subPkg)
                );
                if (created[0] != null) {
                    targetDir = created[0];
                }
            }
        }
        PsiPackage pkg = JavaDirectoryService.getInstance().getPackage(targetDir);
        String packageName = pkg != null ? pkg.getQualifiedName() : "";
        Map<String, String> files = generateSources(patternConfig, packageName);
        PsiFileWriter.writeFiles(project,targetDir,"Générer " + patternConfig.pattern().displayName(),files);
    }

    /**
     * Génère en mémoire l'ensemble des fichiers sources d'un patron sans dépendance au SDK IntelliJ.
     * Utilisable directement pour les tests unitaires JUnit.
     *
     * @param pattern     patron de conception à générer
     * @param baseName    nom de base saisi par l'utilisateur
     * @param packageName nom qualifié du package cible
     * @param pedagogical {@code true} pour inclure les commentaires pédagogiques
     * @return table associant chaque nom de fichier ({@code Nom.java}) à son code source
     */
    public static Map<String, String> generateSources(DesignPatternType pattern,
                                                      String baseName,
                                                      String packageName,
                                                      boolean pedagogical) {
        java.util.Set<String> allOptionals = pattern.getRoles().stream()
                .filter(r -> !r.required())
                .map(PatternRole::id)
                .collect(java.util.stream.Collectors.toSet());

        PatternGenerationConfig defaultConfig = new PatternGenerationConfig(
                pattern, baseName, false, pedagogical, allOptionals, Map.of(), List.of()
        );
        return generateSources(defaultConfig, packageName);
    }

    /**
     * Génère en mémoire les sources Java selon une {@link PatternGenerationConfig} complète.
     */
    public static Map<String, String> generateSources(PatternGenerationConfig config, String packageName) {
        DesignPatternType pattern = config.pattern();
        Map<String, List<String>> roleClasses = resolveAllClassNames(config);
        GenerationContext ctx = new GenerationContext(
                pattern,
                config.baseName(),
                packageName,
                roleClasses,
                config.enabledOptionalRoleIds(),
                config.roleInstanceCounts(),
                config.variants(),
                config.pedagogical()
        );
        PatternStrategy strategy = CUSTOM_STRATEGIES.getOrDefault(pattern, DEFAULT_STRATEGY);

        Map<String, String> files = new LinkedHashMap<>();
        for (PatternRole role : pattern.getRoles()) {
            for (String className : ctx.allClassesOf(role.id())) {
                JavaClassBuilder builder = new JavaClassBuilder(packageName, role.kind(), className);
                strategy.buildRole(role, className, builder, ctx);
                files.put(className + ".java", builder.build());
            }
        }
        return files;
    }


    /**
     * Résout et indexe les noms de classes générés pour chacun des rôles d'un patron.
     *
     * @param config paramètre choisi dans la boite de dialogue
     * @return correspondance ordonnée {@code roleId -> liste des noms de classes}
     */
    private static Map<String, List<String>> resolveAllClassNames(PatternGenerationConfig config) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        List<String> variants = config.variants() != null ? config.variants() : List.of();

        for (PatternRole role : config.pattern().getRoles()) {
            if (!role.required() && !config.enabledOptionalRoleIds().contains(role.id())) {
                continue;
            }

            if (role.multiple()) {
                int spinnerCount = config.roleInstanceCounts().getOrDefault(role.id(), role.defaultInstances());
                int effectiveCount = Math.max(1, spinnerCount);

                List<String> names = new ArrayList<>();
                for (int i = 1; i <= effectiveCount; i++) {
                    names.add(resolveRoleNameWithVariants(role, config.baseName(), i, variants));
                }
                map.put(role.id(), names);
            } else {
                map.put(role.id(), List.of(role.resolveName(config.baseName(), 1)));
            }
        }
        return map;
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
