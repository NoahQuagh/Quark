package com.noahquagh.quark.generator;

import com.noahquagh.quark.model.DesignPatternType;
import com.noahquagh.quark.model.PatternGenerationConfig;
import com.noahquagh.quark.model.PatternRole;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Options de configuration : r\u00f4les facultatifs, nombre d'instances, variantes nomm\u00e9es,
 * mode p\u00e9dagogique et injection du client dans la classe active.
 */
class PatternVariantsAndOptionsTest {

    private static final String PACKAGE = "com.test.pattern";
    private static final String TODO = "// TODO : Impl\u00e9menter cette m\u00e9thode";

    // R\u00f4les facultatifs

    @ParameterizedTest
    @EnumSource(DesignPatternType.class)
    void optionalRolesAreOmittedWhenNotEnabled(DesignPatternType pattern) {
        Map<String, String> sources = generate(config(pattern, "Demo", Set.of(), Map.of(), List.of(), false, false));

        List<String> expected = new ArrayList<>();
        for (PatternRole role : pattern.getRoles()) {
            if (role.required()) {
                expected.addAll(classNames(role, "Demo", role.defaultInstances()));
            }
        }
        assertEquals(withJavaExtension(expected), new ArrayList<>(sources.keySet()));
    }

    @ParameterizedTest
    @EnumSource(DesignPatternType.class)
    void optionalRolesAreGeneratedWhenEnabled(DesignPatternType pattern) {
        Set<String> allOptionals = optionalRoleIds(pattern);
        Map<String, String> sources = generate(config(pattern, "Demo", allOptionals, Map.of(), List.of(), false, false));

        List<String> expected = new ArrayList<>();
        for (PatternRole role : pattern.getRoles()) {
            expected.addAll(classNames(role, "Demo", role.defaultInstances()));
        }
        assertEquals(withJavaExtension(expected), new ArrayList<>(sources.keySet()));
    }

    @Test
    void onlyTheSelectedOptionalRoleIsGenerated() {
        Map<String, String> sources = generate(config(DesignPatternType.PROTOTYPE, "Demo",
                Set.of("REGISTRY"), Map.of(), List.of(), false, false));

        assertTrue(sources.containsKey("DemoPrototypeRegistry.java"));
        assertFalse(sources.containsKey("DemoClient.java"));
        assertEquals(List.of("DemoPrototype.java", "ConcreteDemoPrototypeA.java", "ConcreteDemoPrototypeB.java",
                "DemoPrototypeRegistry.java"), new ArrayList<>(sources.keySet()));
    }

    @Test
    void disablingAnOptionalRoleRemovesItsFileOnly() {
        Map<String, String> without = generate(config(DesignPatternType.FACTORY_METHOD, "Demo",
                Set.of(), Map.of(), List.of(), false, false));
        Map<String, String> with = generate(config(DesignPatternType.FACTORY_METHOD, "Demo",
                Set.of("CLIENT"), Map.of(), List.of(), false, false));

        assertFalse(without.containsKey("DemoClient.java"));
        assertTrue(with.containsKey("DemoClient.java"));
        assertEquals(with.size() - 1, without.size());
        assertTrue(with.keySet().containsAll(without.keySet()));
    }

    // R\u00f4les multiples : roleInstanceCounts

    @Test
    void customInstanceCountGeneratesThreeStrategiesInsteadOfTwo() {
        Map<String, String> sources = generate(config(DesignPatternType.STRATEGY, "Demo",
                Set.of(), Map.of("CONCRETE_STRATEGY", 3), List.of(), false, false));

        assertEquals(List.of("DemoStrategy.java", "ConcreteDemoStrategyA.java", "ConcreteDemoStrategyB.java",
                "ConcreteDemoStrategyC.java", "DemoContext.java"), new ArrayList<>(sources.keySet()));
        assertTrue(sources.get("ConcreteDemoStrategyC.java")
                .contains("public class ConcreteDemoStrategyC implements DemoStrategy {"));
    }

    @Test
    void customInstanceCountOfOneGeneratesASingleConcreteClass() {
        Map<String, String> sources = generate(config(DesignPatternType.STRATEGY, "Demo",
                Set.of(), Map.of("CONCRETE_STRATEGY", 1), List.of(), false, false));

        assertEquals(List.of("DemoStrategy.java", "ConcreteDemoStrategyA.java", "DemoContext.java"),
                new ArrayList<>(sources.keySet()));
    }

    @Test
    void anInstanceCountBelowOneFallsBackToOneInstance() {
        Map<String, String> sources = generate(config(DesignPatternType.STRATEGY, "Demo",
                Set.of(), Map.of("CONCRETE_STRATEGY", 0), List.of(), false, false));

        assertTrue(sources.containsKey("ConcreteDemoStrategyA.java"));
        assertFalse(sources.containsKey("ConcreteDemoStrategyB.java"));
    }

    @Test
    void visitorMultipliesVisitMethodsWithTheNumberOfConcreteElements() {
        Map<String, String> sources = generate(config(DesignPatternType.VISITOR, "Demo",
                Set.of(), Map.of("CONCRETE_ELEMENT", 3), List.of(), false, false));

        String visitor = sources.get("DemoVisitor.java");
        for (String element : List.of("A", "B", "C")) {
            assertTrue(visitor.contains("    void visit(ConcreteDemoElement" + element + " element);"));
        }
        assertEquals(3, count(visitor, "void visit("));

        String concreteVisitor = sources.get("ConcreteDemoVisitorA.java");
        assertEquals(3, count(concreteVisitor, "    @Override\n    public void visit(ConcreteDemoElement"));
        assertTrue(sources.containsKey("ConcreteDemoElementC.java"));
    }

    // Variantes nomm\u00e9es

    @Test
    void namedVariantsReplaceTheLetterPlaceholder() {
        Map<String, String> sources = generate(config(DesignPatternType.STRATEGY, "Payment",
                Set.of(), Map.of(), List.of("PayPal", "Stripe"), false, false));

        assertEquals(List.of("PaymentStrategy.java", "ConcretePaymentStrategyPayPal.java",
                "ConcretePaymentStrategyStripe.java", "PaymentContext.java"), new ArrayList<>(sources.keySet()));
        assertTrue(sources.get("ConcretePaymentStrategyPayPal.java")
                .contains("public class ConcretePaymentStrategyPayPal implements PaymentStrategy {"));
        assertTrue(sources.get("ConcretePaymentStrategyStripe.java")
                .contains("public class ConcretePaymentStrategyStripe implements PaymentStrategy {"));
    }

    @Test
    void namedVariantsReplaceTheIndexPlaceholder() {
        Map<String, String> sources = generate(config(DesignPatternType.ABSTRACT_FACTORY, "Shop",
                Set.of(), Map.of(), List.of("PayPal", "Stripe"), false, false));

        assertEquals(List.of("ShopFactory.java", "ConcreteShopFactoryPayPal.java", "ConcreteShopFactoryStripe.java",
                "ShopProductA.java", "ShopProductB.java",
                "ConcreteShopProductAPayPal.java", "ConcreteShopProductAStripe.java",
                "ConcreteShopProductBPayPal.java", "ConcreteShopProductBStripe.java"),
                new ArrayList<>(sources.keySet()));
        assertFalse(sources.containsKey("ConcreteShopFactory1.java"));
    }

    @Test
    void variantsAreSanitizedAndCapitalized() {
        Map<String, String> sources = generate(config(DesignPatternType.STRATEGY, "Payment",
                Set.of(), Map.of(), List.of("pay pal", "stripe-2"), false, false));

        assertTrue(sources.containsKey("ConcretePaymentStrategyPaypal.java"));
        assertTrue(sources.containsKey("ConcretePaymentStrategyStripe2.java"));
    }

    @Test
    void missingOrUnusableVariantsFallBackToLetters() {
        Map<String, String> fewer = generate(config(DesignPatternType.STRATEGY, "Payment",
                Set.of(), Map.of(), List.of("PayPal"), false, false));
        assertTrue(fewer.containsKey("ConcretePaymentStrategyPayPal.java"));
        assertTrue(fewer.containsKey("ConcretePaymentStrategyB.java"));

        Map<String, String> unusable = generate(config(DesignPatternType.STRATEGY, "Payment",
                Set.of(), Map.of(), List.of("!!!", "Stripe"), false, false));
        assertTrue(unusable.containsKey("ConcretePaymentStrategyA.java"));
        assertTrue(unusable.containsKey("ConcretePaymentStrategyStripe.java"));
    }

    @Test
    void variantsBeyondTheInstanceCountAreIgnoredUnlessTheCountIsRaised() {
        List<String> variants = List.of("PayPal", "Stripe", "Adyen");

        Map<String, String> byDefault = generate(config(DesignPatternType.STRATEGY, "Payment",
                Set.of(), Map.of(), variants, false, false));
        assertFalse(byDefault.containsKey("ConcretePaymentStrategyAdyen.java"));

        Map<String, String> raised = generate(config(DesignPatternType.STRATEGY, "Payment",
                Set.of(), Map.of("CONCRETE_STRATEGY", 3), variants, false, false));
        assertTrue(raised.containsKey("ConcretePaymentStrategyAdyen.java"));
    }

    // Mode p\u00e9dagogique

    @ParameterizedTest
    @EnumSource(DesignPatternType.class)
    void nonPedagogicalModeProducesNoJavadocNorTodo(DesignPatternType pattern) {
        PatternGenerator.generateSources(pattern, "Demo", PACKAGE, false).forEach((file, code) -> {
            assertFalse(code.contains("/**"), "Javadoc inattendue dans " + pattern + " / " + file);
            assertFalse(code.contains("TODO"), "TODO inattendu dans " + pattern + " / " + file);
        });
    }

    @ParameterizedTest
    @EnumSource(DesignPatternType.class)
    void pedagogicalModeAddsRoleJavadocBeforeEveryDeclaration(DesignPatternType pattern) {
        Map<String, String> sources = PatternGenerator.generateSources(pattern, "Demo", PACKAGE, true);

        for (PatternRole role : pattern.getRoles()) {
            for (String className : classNames(role, "Demo", role.defaultInstances())) {
                String code = sources.get(className + ".java");
                String where = pattern + " / " + className;

                assertTrue(code.contains("/**\n"), "Javadoc absente : " + where);
                if (pattern == DesignPatternType.SINGLETON) {
                    assertTrue(code.contains(" * Pattern Singleton : garantit une instance unique de DemoSingleton.\n"), where);
                } else {
                    assertTrue(code.contains(" * Pattern " + pattern.displayName() + " (" + pattern.getCategory() + ")\n"), where);
                    assertTrue(code.contains(" * <p>" + pattern.getDescription() + "</p>\n"), where);
                    assertTrue(code.contains(" * <p>R\u00f4le : " + role.description() + "</p>\n"), where);
                }
                List<String> lines = List.of(code.split("\n", -1));
                int declaration = indexOfDeclaration(lines, className);
                assertEquals(" */", lines.get(declaration - 1), "la Javadoc doit pr\u00e9c\u00e9der la d\u00e9claration : " + where);
            }
        }
    }

    @Test
    void pedagogicalModeAddsTodoOnlyInMethodBodies() {
        Map<String, String> sources = PatternGenerator.generateSources(DesignPatternType.STRATEGY, "Demo", PACKAGE, true);

        assertEquals(0, count(sources.get("DemoStrategy.java"), "TODO"), "une interface n'a pas de corps");
        assertEquals(1, count(sources.get("ConcreteDemoStrategyA.java"), TODO));
        assertEquals(2, count(sources.get("DemoContext.java"), TODO));
    }

    @Test
    void pedagogicalFlagChangesNothingExceptCommentsAndTodos() {
        Map<String, String> plain = PatternGenerator.generateSources(DesignPatternType.STRATEGY, "Demo", PACKAGE, false);
        Map<String, String> pedagogical = PatternGenerator.generateSources(DesignPatternType.STRATEGY, "Demo", PACKAGE, true);

        assertEquals(new ArrayList<>(plain.keySet()), new ArrayList<>(pedagogical.keySet()));
        pedagogical.forEach((file, code) -> {
            String stripped = code.replaceAll("(?s)/\\*\\*.*?\\*/\n", "").replace("        " + TODO + "\n", "");
            assertEquals(plain.get(file), stripped, file);
        });
    }

    // injectClientIntoActiveClass

    @ParameterizedTest
    @EnumSource(DesignPatternType.class)
    void injectedClientRoleIsNotGeneratedAsASeparateFile(DesignPatternType pattern) {
        Map<String, String> sources = generate(config(pattern, "Demo", optionalRoleIds(pattern), Map.of(), List.of(), false, true));

        String clientRoleId = pattern.findClientRole().map(PatternRole::id).orElse(null);
        List<String> expected = new ArrayList<>();
        for (PatternRole role : pattern.getRoles()) {
            if (!role.id().equals(clientRoleId)) {
                expected.addAll(classNames(role, "Demo", role.defaultInstances()));
            }
        }
        assertEquals(withJavaExtension(expected), new ArrayList<>(sources.keySet()));
    }

    @Test
    void injectedClientKeepsTheOtherRolesOfTheStrategyPattern() {
        Map<String, String> sources = generate(config(DesignPatternType.STRATEGY, "Demo",
                Set.of(), Map.of(), List.of(), false, true));

        assertEquals(List.of("DemoStrategy.java", "ConcreteDemoStrategyA.java", "ConcreteDemoStrategyB.java"),
                new ArrayList<>(sources.keySet()));
        assertFalse(sources.containsKey("DemoContext.java"));
    }

    @Test
    void clientIsGeneratedWhenInjectionIsDisabled() {
        Map<String, String> sources = generate(config(DesignPatternType.FACTORY_METHOD, "Demo",
                Set.of("CLIENT"), Map.of(), List.of(), false, false));

        assertTrue(sources.containsKey("DemoClient.java"));
        assertTrue(sources.get("DemoClient.java").contains("    private DemoCreator demoCreator;"));
    }

    /**
     * Bug connu du g\u00e9n\u00e9rateur : pour State, le r\u00f4le client ({@code CONTEXT}) est r\u00e9f\u00e9renc\u00e9 par
     * {@code void handle({CONTEXT} context)}. Quand il est inject\u00e9 dans une classe existante, aucun fichier ne
     * le porte et le placeholder n'est pas r\u00e9solu : le code g\u00e9n\u00e9r\u00e9 contient {@code handle({CONTEXT} context)}
     * et ne compile pas. \u00c0 r\u00e9activer une fois le g\u00e9n\u00e9rateur corrig\u00e9.
     */
    @Disabled("Bug connu : placeholder {CONTEXT} non r\u00e9solu quand le client de State est inject\u00e9")
    @Test
    void injectedClientLeavesNoUnresolvedPlaceholderInState() {
        Map<String, String> sources = generate(config(DesignPatternType.STATE, "Demo",
                Set.of(), Map.of(), List.of(), false, true));

        sources.forEach((file, code) -> {
            Matcher matcher = Pattern.compile("\\{[A-Z0-9_]+}").matcher(code);
            if (matcher.find()) {
                fail("placeholder non r\u00e9solu " + matcher.group() + " dans " + file);
            }
        });
    }

    // Aides

    private static PatternGenerationConfig config(DesignPatternType pattern, String baseName, Set<String> optionalRoleIds,
                                                  Map<String, Integer> counts, List<String> variants,
                                                  boolean pedagogical, boolean injectClient) {
        return new PatternGenerationConfig(pattern, baseName, false, pedagogical, optionalRoleIds, counts, variants, injectClient);
    }

    private static Map<String, String> generate(PatternGenerationConfig config) {
        return PatternGenerator.generateSources(config, PACKAGE);
    }

    private static Set<String> optionalRoleIds(DesignPatternType pattern) {
        return pattern.getRoles().stream()
                .filter(r -> !r.required())
                .map(PatternRole::id)
                .collect(java.util.stream.Collectors.toSet());
    }

    private static List<String> classNames(PatternRole role, String baseName, int instances) {
        List<String> names = new ArrayList<>();
        for (int i = 1; i <= Math.max(1, instances); i++) {
            names.add(role.resolveName(baseName, i));
        }
        return names;
    }

    private static List<String> withJavaExtension(List<String> classNames) {
        return classNames.stream().map(n -> n + ".java").toList();
    }

    private static int indexOfDeclaration(List<String> lines, String className) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("public ") && line.contains(" " + className + " ")) {
                return i;
            }
        }
        throw new AssertionError("d\u00e9claration introuvable pour " + className);
    }

    private static int count(String text, String needle) {
        int count = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            count++;
        }
        return count;
    }
}
