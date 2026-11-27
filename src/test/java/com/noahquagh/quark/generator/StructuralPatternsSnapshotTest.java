package com.noahquagh.quark.generator;

import com.noahquagh.quark.model.DesignPatternType;
import com.noahquagh.quark.model.PatternCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Golden Master des patterns structurels (dont Composite : {@code List<...>} et import {@code java.util.List}).
 */
class StructuralPatternsSnapshotTest {

    @ParameterizedTest
    @EnumSource(value = DesignPatternType.class,
            names = {"ADAPTER", "BRIDGE", "COMPOSITE", "DECORATOR", "FACADE", "FLYWEIGHT", "PROXY"})
    void generatedSourcesMatchGoldenMaster(DesignPatternType pattern) {
        GoldenMaster.assertMatches(GoldenMaster.scenarioOf(pattern),
                PatternGenerator.generateSources(pattern, "Demo", "com.test.pattern", false));
    }

    @Test
    void everyStructuralPatternHasAGoldenMaster() {
        GoldenMaster.assertCategoryFullyCovered(StructuralPatternsSnapshotTest.class,
                "generatedSourcesMatchGoldenMaster", PatternCategory.STRUCTURAL);
    }
}
