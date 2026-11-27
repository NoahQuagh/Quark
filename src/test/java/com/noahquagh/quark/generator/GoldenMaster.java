package com.noahquagh.quark.generator;

import com.noahquagh.quark.model.DesignPatternType;
import com.noahquagh.quark.model.PatternCategory;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Utilitaire de test : compare le code g\u00e9n\u00e9r\u00e9 \u00e0 des "Golden Masters" stock\u00e9s dans
 * {@code src/test/resources/golden/<scenario>/<Classe>.java.txt}.
 *
 * <p>Chaque sc\u00e9nario contient aussi un manifeste {@code _files.txt} qui liste, dans l'ordre,
 * les fichiers attendus : un fichier en trop, manquant ou d\u00e9plac\u00e9 fait \u00e9chouer le test.
 *
 * <p>Pour r\u00e9g\u00e9n\u00e9rer volontairement les attendus apr\u00e8s une modification assum\u00e9e du g\u00e9n\u00e9rateur :
 * {@code ./gradlew test -Dquark.golden.update=true}, puis relire le diff Git avant de commiter.
 */
final class GoldenMaster {

    static final String UPDATE_PROPERTY = "quark.golden.update";

    private static final String CLASSPATH_ROOT = "/golden/";
    private static final Path SOURCE_ROOT = Path.of("src", "test", "resources", "golden");
    private static final String MANIFEST = "_files.txt";

    private GoldenMaster() {
    }

    /** Normalise uniquement les fins de ligne et les blancs de d\u00e9but/fin. */
    static String normalize(String code) {
        return code.replace("\r\n", "\n").trim();
    }

    /** Nom de dossier Golden Master d'un pattern (ex : {@code CHAIN_OF_RESPONSIBILITY} &rarr; {@code chain_of_responsibility}). */
    static String scenarioOf(DesignPatternType pattern) {
        return pattern.name().toLowerCase();
    }

    static void assertMatches(String scenario, Map<String, String> generated) {
        if (Boolean.getBoolean(UPDATE_PROPERTY)) {
            write(scenario, generated);
            return;
        }

        List<String> expectedFiles = readLines(scenario, MANIFEST);
        assertEquals(expectedFiles, new ArrayList<>(generated.keySet()),
                "Liste (ou ordre) des fichiers g\u00e9n\u00e9r\u00e9s diff\u00e9rente du Golden Master '" + scenario + "'");

        for (Map.Entry<String, String> entry : generated.entrySet()) {
            String expected = normalize(read(scenario, entry.getKey() + ".txt"));
            assertEquals(expected, normalize(entry.getValue()),
                    "Golden Master diff\u00e9rent pour " + scenario + "/" + entry.getKey());
        }
    }

    /**
     * V\u00e9rifie que les valeurs {@code names} de l'{@link EnumSource} de la m\u00e9thode param\u00e9tr\u00e9e
     * couvrent exactement les patterns de la cat\u00e9gorie donn\u00e9e (ni oubli, ni intrus).
     */
    static void assertCategoryFullyCovered(Class<?> testClass, String methodName, PatternCategory category) {
        Method method;
        try {
            method = testClass.getDeclaredMethod(methodName, DesignPatternType.class);
        } catch (NoSuchMethodException e) {
            fail("M\u00e9thode introuvable : " + methodName);
            return;
        }
        Set<String> covered = Arrays.stream(method.getAnnotation(EnumSource.class).names())
                .collect(Collectors.toSet());
        Set<String> expected = Stream.of(DesignPatternType.values())
                .filter(p -> p.getCategory() == category)
                .map(Enum::name)
                .collect(Collectors.toSet());
        assertEquals(expected, covered, "Patterns " + category + " non couverts (ou en trop) par les Golden Masters");
    }

    // Lecture / \u00e9criture

    private static void write(String scenario, Map<String, String> generated) {
        Path dir = SOURCE_ROOT.resolve(scenario);
        try {
            Files.createDirectories(dir);
            try (Stream<Path> existing = Files.list(dir)) {
                for (Path p : (Iterable<Path>) existing::iterator) {
                    Files.delete(p);
                }
            }
            for (Map.Entry<String, String> entry : generated.entrySet()) {
                Files.writeString(dir.resolve(entry.getKey() + ".txt"), normalize(entry.getValue()) + "\n", StandardCharsets.UTF_8);
            }
            Files.writeString(dir.resolve(MANIFEST), String.join("\n", generated.keySet()) + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'\u00e9crire le Golden Master " + scenario, e);
        }
    }

    private static List<String> readLines(String scenario, String file) {
        return normalize(read(scenario, file)).lines().filter(l -> !l.isBlank()).toList();
    }

    private static String read(String scenario, String file) {
        String resource = CLASSPATH_ROOT + scenario + "/" + file;
        try (InputStream in = GoldenMaster.class.getResourceAsStream(resource)) {
            if (in == null) {
                fail("Golden Master introuvable : " + resource
                        + " (g\u00e9n\u00e8re-le avec ./gradlew test -D" + UPDATE_PROPERTY + "=true)");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Lecture impossible : " + resource, e);
        }
    }
}
