package com.noahquagh.quark.generator;

import com.noahquagh.quark.model.DesignPatternType;
import com.noahquagh.quark.model.PatternCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Golden Master des patterns comportementaux (dont Visitor : double dispatch et multiplication des
 * m\u00e9thodes {@code visit(...)}, TemplateMethod : {@code final} / {@code abstract}).
 */
class BehavioralPatternsSnapshotTest {

    @ParameterizedTest
    @EnumSource(value = DesignPatternType.class,
            names = {"CHAIN_OF_RESPONSIBILITY", "COMMAND", "INTERPRETER", "ITERATOR", "MEDIATOR", "MEMENTO",
                    "OBSERVER", "STATE", "STRATEGY", "TEMPLATE_METHOD", "VISITOR"})
    void generatedSourcesMatchGoldenMaster(DesignPatternType pattern) {
        GoldenMaster.assertMatches(GoldenMaster.scenarioOf(pattern),
                PatternGenerator.generateSources(pattern, "Demo", "com.test.pattern", false));
    }

    @Test
    void strategyPedagogicalSourcesMatchGoldenMaster() {
        GoldenMaster.assertMatches("strategy_pedagogical",
                PatternGenerator.generateSources(DesignPatternType.STRATEGY, "Demo", "com.test.pattern", true));
    }

    @Test
    void everyBehavioralPatternHasAGoldenMaster() {
        GoldenMaster.assertCategoryFullyCovered(BehavioralPatternsSnapshotTest.class,
                "generatedSourcesMatchGoldenMaster", PatternCategory.BEHAVIORAL);
    }
}
