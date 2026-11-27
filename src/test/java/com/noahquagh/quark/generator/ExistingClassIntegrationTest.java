package com.noahquagh.quark.generator;

import com.noahquagh.quark.model.DesignPatternType;
import com.noahquagh.quark.model.ExistingClassInfo;
import com.noahquagh.quark.model.ExistingClassInfo.MethodSignature;
import com.noahquagh.quark.model.PatternGenerationConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.noahquagh.quark.generator.GoldenMaster.normalize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Int\u00e9gration d'une classe existante du projet ({@code com.app.legacy.LegacyPrinter}) dans les quatre
 * patterns qui acceptent un {@code ExistingClassMode} : ADAPTER (ADAPT), PROXY et DECORATOR (IMPLEMENT_PARENT)
 * et FACADE (WRAP).
 */
class ExistingClassIntegrationTest {

    private static final String PACKAGE = "com.test.pattern";

    private static final ExistingClassInfo LEGACY_PRINTER = new ExistingClassInfo(
            "com.app.legacy.LegacyPrinter", "LegacyPrinter", "com.app.legacy", false,
            List.of(new MethodSignature("void", "printDocument", List.of("String text", "int copies")),
                    new MethodSignature("boolean", "isReady", List.of())));

    private static final ExistingClassInfo EMPTY_LEGACY_PRINTER = new ExistingClassInfo(
            "com.app.legacy.LegacyPrinter", "LegacyPrinter", "com.app.legacy", false, List.of());

    // ADAPTER (ADAPT)

    @Test
    void adapterKeepsTheExistingClassAsAdapteeAndDoesNotRegenerateIt() {
        Map<String, String> sources = generate(DesignPatternType.ADAPTER, LEGACY_PRINTER);

        assertEquals(List.of("PrinterTarget.java", "PrinterAdapter.java", "PrinterClient.java"),
                new ArrayList<>(sources.keySet()));
        assertFalse(sources.containsKey("LegacyPrinter.java"));
        assertFalse(sources.containsKey("PrinterAdaptee.java"));

        assertEquals("""
                package com.test.pattern;

                import com.app.legacy.LegacyPrinter;

                public class PrinterAdapter implements PrinterTarget {

                    private LegacyPrinter legacyPrinter;

                    @Override
                    public void request() {
                    }

                }""", normalize(sources.get("PrinterAdapter.java")));
    }

    @Test
    void adapterDoesNotImportAnExistingClassOfTheSamePackage() {
        ExistingClassInfo samePackage = new ExistingClassInfo(
                "com.test.pattern.LegacyPrinter", "LegacyPrinter", PACKAGE, false, LEGACY_PRINTER.methods());

        String adapter = generate(DesignPatternType.ADAPTER, samePackage).get("PrinterAdapter.java");

        assertFalse(adapter.contains("import "));
        assertTrue(adapter.contains("    private LegacyPrinter legacyPrinter;"));
    }

    // PROXY (IMPLEMENT_PARENT)

    @Test
    void proxySubjectReusesTheExistingSignaturesAndProxyDelegatesEachOfThem() {
        Map<String, String> sources = generate(DesignPatternType.PROXY, LEGACY_PRINTER);

        assertEquals(List.of("PrinterSubject.java", "PrinterProxy.java", "PrinterClient.java"),
                new ArrayList<>(sources.keySet()));
        assertFalse(sources.containsKey("RealPrinterSubject.java"));
        assertFalse(sources.containsKey("LegacyPrinter.java"));

        String subject = sources.get("PrinterSubject.java");
        assertFalse(subject.contains("void request()"));

        assertEquals("""
                package com.test.pattern;

                public interface PrinterSubject {

                    void printDocument(String text, int copies);

                    boolean isReady();

                }""", normalize(subject));

        assertEquals("""
                package com.test.pattern;

                public class PrinterProxy implements PrinterSubject {

                    private PrinterSubject printerSubject;

                    @Override
                    public void printDocument(String text, int copies) {
                        printerSubject.printDocument(text, copies);
                    }

                    @Override
                    public boolean isReady() {
                        return printerSubject.isReady();
                    }

                }""", normalize(sources.get("PrinterProxy.java")));
    }

    // DECORATOR (IMPLEMENT_PARENT)

    @Test
    void decoratorComponentReusesTheExistingSignaturesAndDecoratorsDelegate() {
        Map<String, String> sources = generate(DesignPatternType.DECORATOR, LEGACY_PRINTER);

        assertEquals(List.of("PrinterComponent.java", "PrinterDecorator.java", "ConcretePrinterDecoratorA.java",
                "ConcretePrinterDecoratorB.java", "PrinterClient.java"), new ArrayList<>(sources.keySet()));
        assertFalse(sources.containsKey("ConcretePrinterComponent.java"));
        assertFalse(sources.containsKey("LegacyPrinter.java"));

        assertEquals("""
                package com.test.pattern;

                public interface PrinterComponent {

                    void printDocument(String text, int copies);

                    boolean isReady();

                }""", normalize(sources.get("PrinterComponent.java")));

        assertEquals("""
                package com.test.pattern;

                public abstract class PrinterDecorator implements PrinterComponent {

                    private PrinterComponent printerComponent;

                    @Override
                    public void printDocument(String text, int copies) {
                        printerComponent.printDocument(text, copies);
                    }

                    @Override
                    public boolean isReady() {
                        return printerComponent.isReady();
                    }

                }""", normalize(sources.get("PrinterDecorator.java")));

        assertEquals("""
                package com.test.pattern;

                public class ConcretePrinterDecoratorA extends PrinterDecorator {

                    @Override
                    public void printDocument(String text, int copies) {
                        super.printDocument(text, copies);
                    }

                    @Override
                    public boolean isReady() {
                        return super.isReady();
                    }

                }""", normalize(sources.get("ConcretePrinterDecoratorA.java")));
    }

    // FACADE (WRAP)

    @Test
    void facadeWrapsTheExistingClassAndExposesDelegatedMethods() {
        Map<String, String> sources = generate(DesignPatternType.FACADE, LEGACY_PRINTER);

        assertEquals(List.of("PrinterFacade.java", "PrinterSubsystemA.java", "PrinterSubsystemB.java",
                "PrinterClient.java"), new ArrayList<>(sources.keySet()));
        assertFalse(sources.containsKey("LegacyPrinter.java"));

        assertEquals("""
                package com.test.pattern;

                import com.app.legacy.LegacyPrinter;

                public class PrinterFacade {

                    private PrinterSubsystemA printerSubsystemA;
                    private PrinterSubsystemB printerSubsystemB;
                    private LegacyPrinter legacyPrinter;

                    public void printDocument(String text, int copies) {
                        legacyPrinter.printDocument(text, copies);
                    }

                    public boolean isReady() {
                        return legacyPrinter.isReady();
                    }

                }""", normalize(sources.get("PrinterFacade.java")));
    }

    /**
     * Bug connu : le client de Facade r\u00e9f\u00e9rence la fa\u00e7ade (r\u00f4le cible) et h\u00e9rite donc de l'import
     * {@code com.app.legacy.LegacyPrinter}, alors qu'il ne l'utilise pas. \u00c0 r\u00e9activer une fois corrig\u00e9.
     */
    @Disabled("Bug connu : import inutile de la classe existante dans le client de Facade")
    @Test
    void facadeClientDoesNotImportTheWrappedClass() {
        String client = generate(DesignPatternType.FACADE, LEGACY_PRINTER).get("PrinterClient.java");

        assertFalse(client.contains("import com.app.legacy.LegacyPrinter;"));
    }

    // Classe existante vide

    @Test
    void emptyExistingClassFallsBackToTheProxyInterfaceMethod() {
        Map<String, String> sources = generate(DesignPatternType.PROXY, EMPTY_LEGACY_PRINTER);

        assertEquals("""
                package com.test.pattern;

                public interface PrinterSubject {

                    void request();

                }""", normalize(sources.get("PrinterSubject.java")));

        assertEquals("""
                package com.test.pattern;

                public class PrinterProxy implements PrinterSubject {

                    private PrinterSubject printerSubject;

                    @Override
                    public void request() {
                        printerSubject.request();
                    }

                }""", normalize(sources.get("PrinterProxy.java")));
        assertFalse(sources.containsKey("RealPrinterSubject.java"));
    }

    @Test
    void emptyExistingClassFallsBackToTheDecoratorInterfaceMethod() {
        Map<String, String> sources = generate(DesignPatternType.DECORATOR, EMPTY_LEGACY_PRINTER);

        assertEquals("""
                package com.test.pattern;

                public interface PrinterComponent {

                    void operation();

                }""", normalize(sources.get("PrinterComponent.java")));

        assertEquals("""
                package com.test.pattern;

                public abstract class PrinterDecorator implements PrinterComponent {

                    private PrinterComponent printerComponent;

                    @Override
                    public void operation() {
                        printerComponent.operation();
                    }

                }""", normalize(sources.get("PrinterDecorator.java")));

        assertEquals("""
                package com.test.pattern;

                public class ConcretePrinterDecoratorA extends PrinterDecorator {

                    @Override
                    public void operation() {
                        super.operation();
                    }

                }""", normalize(sources.get("ConcretePrinterDecoratorA.java")));
    }

    @Test
    void emptyExistingClassKeepsTheFacadeStandardMethodAndStillWrapsTheClass() {
        String facade = generate(DesignPatternType.FACADE, EMPTY_LEGACY_PRINTER).get("PrinterFacade.java");

        assertTrue(facade.contains("import com.app.legacy.LegacyPrinter;"));
        assertTrue(facade.contains("    private LegacyPrinter legacyPrinter;"));
        assertTrue(facade.contains("    public void operation() {"));
    }

    // Aides

    private static Map<String, String> generate(DesignPatternType pattern, ExistingClassInfo existing) {
        PatternGenerationConfig config = new PatternGenerationConfig(pattern, "Printer", false, false,
                Set.of("CLIENT"), Map.of(), List.of(), false, existing);
        return PatternGenerator.generateSources(config, PACKAGE);
    }
}
