package com.noahquagh.quark.generator;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiPackage;
import com.noahquagh.quark.generator.strategy.DefaultPatternStrategy;
import com.noahquagh.quark.generator.strategy.PatternStrategy;
import com.noahquagh.quark.generator.strategy.SingletonStrategy;
import com.noahquagh.quark.model.DesignPatternType;
import com.noahquagh.quark.model.PatternRole;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PatternGenerator {
    private static final Map<DesignPatternType, PatternStrategy> CUSTOM_STRATEGIES = Map.of(
            DesignPatternType.SINGLETON, new SingletonStrategy()
    );
    private static final PatternStrategy DEFAULT_STRATEGY = new DefaultPatternStrategy();

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

    //todo methode Java uniquement pour test unitair JUnit
    public static Map<String, String> generateSources(DesignPatternType pattern,
                                                      String baseName,
                                                      String packageName,
                                                      boolean pedagogical) {
        Map<String, List<String>> roleClasses = resolveAllClassNames(pattern, baseName);
        GenerationContext ctx = new GenerationContext(pattern, baseName, packageName, roleClasses, pedagogical);
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
}
