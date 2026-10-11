package com.swiftfaze.veil.entities.player.classes;

import com.swiftfaze.veil.component.DetailTable;
import com.swiftfaze.veil.entities.player.Stats;
import com.swiftfaze.veil.testing.property.VeilArbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerClassTest {

    private final PlayerClass testClass = new PlayerClass("test:class", "Test",
            Map.of(
                    "strength", new PlayerClass.StatCurve(10, "level*2"),
                    "dexterity", new PlayerClass.StatCurve(8, null),
                    "maxHp", new PlayerClass.StatCurve(100, null),
                    "maxMana", new PlayerClass.StatCurve(50, null)
            ));

    @Test
    void baseOnlyStatsAtLevel0() {
        PlayerClass baseClass = new PlayerClass("test:base", "Base",
                Map.of(
                        "strength", new PlayerClass.StatCurve(15, null),
                        "dexterity", new PlayerClass.StatCurve(10, null),
                        "constitution", new PlayerClass.StatCurve(14, null),
                        "intelligence", new PlayerClass.StatCurve(6, null),
                        "wisdom", new PlayerClass.StatCurve(6, null),
                        "luck", new PlayerClass.StatCurve(8, null),
                        "maxHp", new PlayerClass.StatCurve(120, null),
                        "maxMana", new PlayerClass.StatCurve(20, null)
                ));

        Stats stats = new Stats();
        baseClass.applyStatsAtLevel(stats, 0);

        assertEquals(15, stats.getStrength());
        assertEquals(120, stats.getMaxHp());
        assertEquals(20, stats.getMaxMana());
    }

    @Test
    void additiveGrowthAtLaterLevel() {
        Stats stats = new Stats();
        testClass.applyStatsAtLevel(stats, 5);

        assertEquals(20, stats.getStrength()); // base 10 + level*2 (5*2=10)
    }

    @Test
    void statWithNoGrowthCurveStaysFlat() {
        Stats stats0 = new Stats();
        testClass.applyStatsAtLevel(stats0, 0);

        Stats stats10 = new Stats();
        testClass.applyStatsAtLevel(stats10, 10);

        assertEquals(stats0.getDexterity(), stats10.getDexterity());
        assertEquals(8, stats10.getDexterity());
    }

    @Test
    void missingBaseDefaultsTo0() {
        PlayerClass sparse = new PlayerClass("test:sparse", "Sparse",
                Map.of("strength", new PlayerClass.StatCurve(0, "level")));

        Stats stats = new Stats();
        sparse.applyStatsAtLevel(stats, 5);

        assertEquals(5, stats.getStrength()); // base 0 + level*1 = 5
    }

    @Test
    void maxHpAndMaxManaCascadeToCurrentValues() {
        Stats stats = new Stats();
        testClass.applyStatsAtLevel(stats, 0);

        assertEquals(100, stats.getMaxHp());
        assertEquals(100, stats.getCurrentHp());
        assertEquals(50, stats.getMaxMana());
        assertEquals(50, stats.getCurrentMana());
    }

    @Test
    void getDetailTablesReturnsIdAndName() {
        PlayerClass warrior = new PlayerClass("test:warrior", "Warrior", Map.of());

        List<DetailTable> tables = warrior.getDetailTables();

        assertEquals(1, tables.size());
        assertEquals("", tables.get(0).label());
        assertEquals(List.of("Field", "Value"), tables.get(0).columnHeaders());
        assertEquals(2, tables.get(0).rows().size());
        assertEquals(List.of("ID", "test:warrior"), tables.get(0).rows().get(0));
        assertEquals(List.of("Name", "Warrior"), tables.get(0).rows().get(1));
    }

    // ========== Property-based tests ==========

    @Provide
    Arbitrary<@Nullable String> growthCalcs() {
        // Generate well-formed growth calculation expressions (or null for no growth curve).
        return VeilArbitraries.calcExpression().injectNull(0.2);
    }

    @Provide
    Arbitrary<Integer> levels() {
        return VeilArbitraries.level();
    }

    @Property
    void applyStatsAtLevelIsDeterministic(
            @ForAll("growthCalcs") @Nullable String growthCalc,
            @ForAll("levels") int level) {
        // Create a PlayerClass with a single stat curve to test.
        PlayerClass testClass = new PlayerClass("test:det", "Determinism",
                Map.of("strength", new PlayerClass.StatCurve(10, growthCalc)));

        // Apply the same curve/level to two fresh Stats instances.
        Stats stats1 = new Stats();
        Stats stats2 = new Stats();

        testClass.applyStatsAtLevel(stats1, level);
        testClass.applyStatsAtLevel(stats2, level);

        // Both must have identical strength values.
        assertEquals(stats1.getStrength(), stats2.getStrength());
    }
}
