package com.noahquagh.quark.generator;

import com.noahquagh.quark.model.DesignPatternType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.tools.*;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de tests automatisés vérifiant la génération et la compilation Java
 * des 23 patrons de conception.
 *
 * @author noahq
 * @version 1.1
 */
public class PatternGeneratorTest {

    @TempDir
    Path tempDir;

    @ParameterizedTest(name = "Génération et compilation de {0}")
    @EnumSource(DesignPatternType.class)
    @DisplayName("Tous les 23 patterns doivent générer du code Java valide et compilable")
    void shouldGenerateCompilableJavaCodeForAllPatterns(DesignPatternType pattern) throws IOException {
        String baseName = "Quark";
        String packageName = "com.noahquagh.generated." + pattern.getSubPackage();

        // génération des sources en mode pédagogique
        Map<String, String> generatedFiles = PatternGenerator.generateSources(
                pattern, baseName, packageName, true
        );

        assertNotNull(generatedFiles, "La map des fichiers générés ne doit pas être nulle");
        assertFalse(generatedFiles.isEmpty(), "Le pattern " + pattern + " doit générer au moins un fichier");

        // vérif structurelles de base
        for (Map.Entry<String, String> entry : generatedFiles.entrySet()) {
            String fileName = entry.getKey();
            String source = entry.getValue();

            assertTrue(fileName.endsWith(".java"), "Le fichier doit avoir l'extension .java : " + fileName);
            assertTrue(source.startsWith("package " + packageName + ";"),
                    "En-tête de package manquant dans " + fileName);
            assertFalse(source.contains("{Name}") || source.contains("{Index}") || source.contains("{Letter}"),
                    "Placeholder non résolu détecté dans " + fileName + " :\n" + source);
            assertFalse(source.matches("(?s).*\\{[A-Z0-9_]+}.*"),
                    "Placeholder de rôle non résolu détecté dans " + fileName + " :\n" + source);
        }

        // compilation réelle via fichiers temporaires sur disque
        assertCompilesCleanly(pattern, packageName, generatedFiles);
    }

    /**
     * Écrit les fichiers dans un dossier temporaire et les compile via {@link JavaCompiler}
     * (compatible à la fois avec javac standard et EclipseCompiler).
     */
    private void assertCompilesCleanly(DesignPatternType pattern, String packageName, Map<String, String> files) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            compiler = new org.eclipse.jdt.internal.compiler.tool.EclipseCompiler();
        }
        assertNotNull(compiler, "JavaCompiler indisponible : exécute les tests avec un JDK complet");

        Path srcRoot = tempDir.resolve(pattern.name() + "_src");
        Path outRoot = tempDir.resolve(pattern.name() + "_out");
        Files.createDirectories(outRoot);

        Path packageDir = packageName.isEmpty()
                ? srcRoot
                : srcRoot.resolve(packageName.replace('.', '/'));
        Files.createDirectories(packageDir);

        List<java.io.File> sourceFiles = new ArrayList<>();
        for (Map.Entry<String, String> entry : files.entrySet()) {
            Path filePath = packageDir.resolve(entry.getKey());
            Files.writeString(filePath, entry.getValue());
            sourceFiles.add(filePath.toFile());
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(sourceFiles);

            List<String> options = List.of(
                    "-d", outRoot.toString(),
                    "-sourcepath", srcRoot.toString(),
                    "-proc:none",
                    "-source", "21",
                    "-target", "21"
            );

            StringWriter compilerOutput = new StringWriter();
            JavaCompiler.CompilationTask task = compiler.getTask(
                    compilerOutput, fileManager, diagnostics, options, null, compilationUnits
            );

            boolean success = Boolean.TRUE.equals(task.call());
            if (!success) {
                String errors = diagnostics.getDiagnostics().stream()
                        .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                        .map(d -> String.format("[%s - ligne %d] %s",
                                d.getSource() != null ? d.getSource().getName() : "inconnu",
                                d.getLineNumber(),
                                d.getMessage(null)))
                        .collect(Collectors.joining("\n"));

                String dump = files.entrySet().stream()
                        .map(e -> "=== " + e.getKey() + " ===\n" + e.getValue())
                        .collect(Collectors.joining("\n"));

                throw new AssertionError("Échec de compilation pour " + pattern.displayName()
                        + " :\n" + errors + "\n\nCode généré :\n" + dump);
            }
        }
    }
}