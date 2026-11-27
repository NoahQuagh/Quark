package com.noahquagh.quark.generator;

import com.noahquagh.quark.model.DesignPatternType;
import com.noahquagh.quark.model.PatternCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Golden Master des patterns cr\u00e9ationnels : le code g\u00e9n\u00e9r\u00e9 doit rester identique, caract\u00e8re pour
 * caract\u00e8re, aux fichiers de {@code src/test/resources/golden/<pattern>/}.
 */
class CreationalPatternsSnapshotTest {

    @ParameterizedTest
    @EnumSource(value = DesignPatternType.class,
            names = {"ABSTRACT_FACTORY", "BUILDER", "FACTORY_METHOD", "PROTOTYPE", "SINGLETON"})
    void generatedSourcesMatchGoldenMaster(DesignPatternType pattern) {
        GoldenMaster.assertMatches(GoldenMaster.scenarioOf(pattern),
                PatternGenerator.generateSources(pattern, "Demo", "com.test.pattern", false));
    }

    @Test
    void singletonPedagogicalSourcesMatchGoldenMaster() {
        GoldenMaster.assertMatches("singleton_pedagogical",
                PatternGenerator.generateSources(DesignPatternType.SINGLETON, "Demo", "com.test.pattern", true));
    }

    @Test
    void everyCreationalPatternHasAGoldenMaster() {
        GoldenMaster.assertCategoryFullyCovered(CreationalPatternsSnapshotTest.class,
                "generatedSourcesMatchGoldenMaster", PatternCategory.CREATIONAL);
    }
}
