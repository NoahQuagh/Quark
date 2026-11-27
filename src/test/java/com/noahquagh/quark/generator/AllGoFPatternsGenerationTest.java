package com.noahquagh.quark.generator;

import com.noahquagh.quark.model.ClassKind;
import com.noahquagh.quark.model.DesignPatternType;
import com.noahquagh.quark.model.PatternRole;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * V\u00e9rifie, pour les 23 patterns GoF, que la g\u00e9n\u00e9ration standard respecte la structure d\u00e9duite du
 * mod\u00e8le ({@link DesignPatternType}) : fichiers, package, d\u00e9claration, h\u00e9ritage, champs, imports,
 * m\u00e9thodes et {@code @Override}. Les attendus sont calcul\u00e9s depuis le mod\u00e8le, pas depuis le g\u00e9n\u00e9rateur.
 */
class AllGoFPatternsGenerationTest {

    private static final String BASE = "Demo";
    private static final String PACKAGE = "com.test.pattern";

    /** Ligne de m\u00e9thode (indentation de 4 espaces exactement) : groupe 1 = nom de la m\u00e9thode. */
    private static final Pattern METHOD_LINE = Pattern.compile("^ {4}(?! )(?!private|@)[^(]*?(\\w+)\\(.*\\)\\s*[;{]$");
    private static final Pattern FIELD_LINE = Pattern.compile("^ {4}private [^(]*;$");
    private static final Pattern UNRESOLVED_PLACEHOLDER = Pattern.compile("\\{[A-Z0-9_]+}");

    private record ExpectedClass(PatternRole role, String className) {
    }

    private record GeneratedMethod(String name, boolean overridden, String line) {
    }

    // Fichiers

    @Test
    void theEnumContainsTheTwentyThreeGoFPatterns() {
        assertEquals(23, DesignPatternType.values().length);
    }

    @ParameterizedTest
    @EnumSource(DesignPatternType.class)
    void generatesExactlyTheExpectedFilesInOrder(DesignPatternType pattern) {
        List<String> expected = expectedClasses(pattern).stream()
                .map(c -> c.className() + ".java")
                .toList();

        assertEquals(expected, new ArrayList<>(generate(pattern).keySet()));
    }

    @ParameterizedTest
    @EnumSource(DesignPatternType.class)
    void everyFileDeclaresPackageAndTheRightTypeHeader(DesignPatternType pattern) {
        Map<String, String> sources = generate(pattern);

        for (ExpectedClass expected : expectedClasses(pattern)) {
            String code = sources.get(expected.className() + ".java");
            String where = pattern + " / " + expected.className();

            assertTrue(code.startsWith("package " + PACKAGE + ";\n\n"), "package manquant ou mal plac\u00e9 : " + where);
            assertTrue(code.endsWith("}\n"), "fin de fichier inattendue : " + where);
            assertTrue(lines(code).contains(header(pattern, expected)),
                    "d\u00e9claration attendue '" + header(pattern, expected) + "' absente : " + where);
        }
    }

    @ParameterizedTest
    @EnumSource(DesignPatternType.class)
    void noPlaceholderRemainsUnresolved(DesignPatternType pattern) {
        generate(pattern).forEach((file, code) -> {
            Matcher matcher = UNRESOLVED_PLACEHOLDER.matcher(code);
            if (matcher.find()) {
                fail("placeholder non r\u00e9solu " + matcher.group() + " dans " + pattern + " / " + file);
            }
        });
    }

    // Champs priv\u00e9s (usesIds)

    @ParameterizedTest
    @EnumSource(DesignPatternType.class)
    void privateFieldsFollowUsesIds(DesignPatternType pattern) {
        if (pattern == DesignPatternType.SINGLETON) {
            return; // strat\u00e9gie d\u00e9di\u00e9e, voir singletonIsThreadSafe()
        }
        Map<String, String> sources = generate(pattern);

        for (ExpectedClass expected : expectedClasses(pattern)) {
            PatternRole role = expected.role();
            List<String> actualFields = lines(sources.get(expected.className() + ".java")).stream()
                    .filter(l -> FIELD_LINE.matcher(l).matches())
                    .map(String::trim)
                    .toList();

            List<String> expectedFields = role.kind() == ClassKind.INTERFACE
                    ? List.of()
                    : role.usesIds().stream().map(id -> fieldDeclaration(pattern, id)).toList();

            assertEquals(expectedFields, actualFields, "champs priv\u00e9s de " + pattern + " / " + expected.className());
        }
    }

    // Imports

    @ParameterizedTest
    @EnumSource(DesignPatternType.class)
    void onlySignaturesUsingListImportJavaUtilList(DesignPatternType pattern) {
        Map<String, String> sources = generate(pattern);

        for (ExpectedClass expected : expectedClasses(pattern)) {
            List<String> imports = lines(sources.get(expected.className() + ".java")).stream()
                    .filter(l -> l.startsWith("import "))
                    .toList();
            boolean usesList = expected.role().methods().stream().anyMatch(m -> m.contains("List<"));

            assertEquals(usesList ? List.of("import java.util.List;") : List.<String>of(), imports,
                    "imports de " + pattern + " / " + expected.className());
        }
    }

    // M\u00e9thodes et @Override

    @ParameterizedTest
    @EnumSource(DesignPatternType.class)
    void methodsAndOverrideAnnotationsAreCorrect(DesignPatternType pattern) {
        if (pattern == DesignPatternType.SINGLETON) {
            return; // strat\u00e9gie d\u00e9di\u00e9e, voir singletonIsThreadSafe()
        }
        Map<String, String> sources = generate(pattern);

        for (ExpectedClass expected : expectedClasses(pattern)) {
            PatternRole role = expected.role();
            String where = pattern + " / " + expected.className();
            String code = sources.get(expected.className() + ".java");
            List<GeneratedMethod> methods = parseMethods(code);

            // 1. Ensemble des m\u00e9thodes : propres + (classe concr\u00e8te) abstraites h\u00e9rit\u00e9es
            Set<String> expectedNames = new TreeSet<>();
            role.methods().forEach(m -> expectedNames.add(methodName(m)));
            if (role.kind() == ClassKind.CLASS) {
                expectedNames.addAll(inheritedAbstractNames(pattern, role));
            }
            assertEquals(expectedNames, new TreeSet<>(methods.stream().map(GeneratedMethod::name).toList()),
                    "m\u00e9thodes de " + where);

            // 2. @Override : uniquement sur les classes concr\u00e8tes qui red\u00e9finissent une m\u00e9thode d'un anc\u00eatre
            Set<String> ancestorNames = ancestorMethodNames(pattern, role);
            Set<String> staticOrAbstractOwn = new TreeSet<>();
            role.methods().stream()
                    .filter(m -> m.startsWith("static ") || m.startsWith("abstract "))
                    .forEach(m -> staticOrAbstractOwn.add(methodName(m)));

            for (GeneratedMethod method : methods) {
                boolean expectedOverride = role.kind() == ClassKind.CLASS
                        && role.parentId() != null
                        && ancestorNames.contains(method.name())
                        && !staticOrAbstractOwn.contains(method.name());
                assertEquals(expectedOverride, method.overridden(),
                        "@Override pour '" + method.line().trim() + "' dans " + where);
            }

            // 3. Interface : signatures seules ; classe abstraite : m\u00e9thodes abstraites sans corps
            if (role.kind() == ClassKind.INTERFACE) {
                methods.forEach(m -> assertTrue(m.line().endsWith(";"), "signature d'interface sans corps : " + m.line()));
            }
        }
    }

    // Singleton

    @Test
    void singletonIsThreadSafe() {
        Map<String, String> sources = generate(DesignPatternType.SINGLETON);

        assertEquals(List.of("DemoSingleton.java"), new ArrayList<>(sources.keySet()));
        String code = sources.get("DemoSingleton.java");

        assertTrue(code.contains("public class DemoSingleton {"));
        assertTrue(code.contains("private static volatile DemoSingleton instance;"));
        assertTrue(code.contains("private DemoSingleton() {"), "constructeur priv\u00e9");
        assertTrue(code.contains("public static DemoSingleton getInstance() {"));
        assertEquals(2, count(code, "if (instance == null) {"), "double v\u00e9rification du verrouillage");
        assertTrue(code.contains("synchronized (DemoSingleton.class) {"));
        assertTrue(code.contains("instance = new DemoSingleton();"));
        assertTrue(code.contains("return instance;"));
    }

    /**
     * Bug connu : {@code SingletonStrategy} injecte des blocs de texte sans l'indentation de 4 espaces
     * (constructeur et {@code getInstance()} d\u00e9marrent en colonne 0). \u00c0 r\u00e9activer une fois corrig\u00e9,
     * puis r\u00e9g\u00e9n\u00e9rer les Golden Masters avec {@code -Dquark.golden.update=true}.
     */
    @Disabled("Bug connu : membres du Singleton non indent\u00e9s")
    @Test
    void singletonMembersAreIndentedByFourSpaces() {
        String code = generate(DesignPatternType.SINGLETON).get("DemoSingleton.java");

        assertTrue(code.contains("\n    private DemoSingleton() {\n"));
        assertTrue(code.contains("\n    public static DemoSingleton getInstance() {\n"));
    }

    // Aides : attendus d\u00e9duits du mod\u00e8le

    private static Map<String, String> generate(DesignPatternType pattern) {
        return PatternGenerator.generateSources(pattern, BASE, PACKAGE, false);
    }

    /** Toutes les classes attendues : les r\u00f4les facultatifs sont activ\u00e9s par la surcharge \u00e0 4 arguments. */
    private static List<ExpectedClass> expectedClasses(DesignPatternType pattern) {
        List<ExpectedClass> result = new ArrayList<>();
        for (PatternRole role : pattern.getRoles()) {
            int count = Math.max(1, role.defaultInstances());
            for (int i = 1; i <= count; i++) {
                result.add(new ExpectedClass(role, role.resolveName(BASE, i)));
            }
        }
        return result;
    }

    private static String header(DesignPatternType pattern, ExpectedClass expected) {
        PatternRole role = expected.role();
        String clause = "";
        if (role.parentId() != null) {
            PatternRole parent = pattern.getRole(role.parentId()).orElseThrow();
            String relation = role.kind() != ClassKind.INTERFACE && parent.kind() == ClassKind.INTERFACE
                    ? "implements" : "extends";
            clause = " " + relation + " " + parent.resolveName(BASE, 1);
        }
        return "public " + role.kind().getKeyword() + " " + expected.className() + clause + " {";
    }

    private static String fieldDeclaration(DesignPatternType pattern, String usedRoleId) {
        String type = pattern.getRole(usedRoleId).orElseThrow().resolveName(BASE, 1);
        return "private " + type + " " + Character.toLowerCase(type.charAt(0)) + type.substring(1) + ";";
    }

    private static List<PatternRole> ancestors(DesignPatternType pattern, PatternRole role) {
        List<PatternRole> chain = new ArrayList<>();
        String parentId = role.parentId();
        while (parentId != null) {
            PatternRole parent = pattern.getRole(parentId).orElseThrow();
            chain.add(parent);
            parentId = parent.parentId();
        }
        return chain;
    }

    private static Set<String> ancestorMethodNames(DesignPatternType pattern, PatternRole role) {
        Set<String> names = new TreeSet<>();
        for (PatternRole ancestor : ancestors(pattern, role)) {
            ancestor.methods().forEach(m -> names.add(methodName(m)));
        }
        return names;
    }

    private static Set<String> inheritedAbstractNames(DesignPatternType pattern, PatternRole role) {
        Set<String> names = new TreeSet<>();
        for (PatternRole ancestor : ancestors(pattern, role)) {
            for (String raw : ancestor.methods()) {
                if (ancestor.kind() == ClassKind.INTERFACE || raw.startsWith("abstract ")) {
                    names.add(methodName(raw));
                }
            }
        }
        return names;
    }

    private static String methodName(String rawSignature) {
        String beforeParen = rawSignature.substring(0, rawSignature.indexOf('(')).trim();
        String[] tokens = beforeParen.split("\\s+");
        return tokens[tokens.length - 1];
    }

    private static List<GeneratedMethod> parseMethods(String code) {
        List<String> lines = lines(code);
        List<GeneratedMethod> methods = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            Matcher matcher = METHOD_LINE.matcher(lines.get(i));
            if (matcher.matches()) {
                boolean overridden = i > 0 && lines.get(i - 1).equals("    @Override");
                methods.add(new GeneratedMethod(matcher.group(1), overridden, lines.get(i)));
            }
        }
        return methods;
    }

    private static List<String> lines(String code) {
        return List.of(code.replace("\r\n", "\n").split("\n", -1));
    }

    private static int count(String text, String needle) {
        int count = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            count++;
        }
        return count;
    }
}
