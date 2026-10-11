package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.player.Stats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassSandboxModelTest {

    private static final int WARRIOR_ATTACK_POWER = 35;
    private static final int WARRIOR_DEFENSE = 17;
    private static final int WARRIOR_MAX_HP = 120;
    private static final int WARRIOR_MAX_MANA = 20;

    @Test
    void listsAllKnownClasses() {
        ClassSandboxModel model = new ClassSandboxModel();

        List<String> names = model.classNames();

        assertTrue(names.contains("Warrior"));
        assertTrue(names.contains("Mage"));
    }

    @Test
    void computesWarriorStats() {
        ClassSandboxModel model = new ClassSandboxModel();

        Stats stats = model.computedStats("Warrior");

        assertEquals(35, stats.getAttackPower());
        assertEquals(17, stats.getDefense());
        assertEquals(120, stats.getMaxHp());
        assertEquals(20, stats.getMaxMana());
    }

    @Test
    void computesMageStats() {
        ClassSandboxModel model = new ClassSandboxModel();

        Stats stats = model.computedStats("Mage");

        assertEquals(16, stats.getAttackPower());
        assertEquals(11, stats.getDefense());
        assertEquals(70, stats.getMaxHp());
        assertEquals(100, stats.getMaxMana());
    }

    @Test
    void looksUpIdByClassName() {
        ClassSandboxModel model = new ClassSandboxModel();

        assertEquals("core:warrior", model.idFor("Warrior"));
        assertEquals("core:mage", model.idFor("Mage"));
    }

    @Test
    void computesStatsAtSpecificLevel() {
        ClassSandboxModel model = new ClassSandboxModel();

        Stats stats = model.computedStats("Warrior", 0);

        assertEquals(List.of(WARRIOR_ATTACK_POWER, WARRIOR_DEFENSE, WARRIOR_MAX_HP, WARRIOR_MAX_MANA),
                List.of(stats.getAttackPower(), stats.getDefense(), stats.getMaxHp(), stats.getMaxMana()));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 5, 10, 15, 20})
    void warriorStatsAreTheSameAtEveryLevelBecauseItHasNoGrowthCurves(int level) {
        ClassSandboxModel model = new ClassSandboxModel();

        Stats stats = model.computedStats("Warrior", level);

        assertEquals(WARRIOR_ATTACK_POWER, stats.getAttackPower(), "Attack Power should be unchanged at level " + level);
        assertEquals(WARRIOR_MAX_HP, stats.getMaxHp(), "Max HP should be unchanged at level " + level);
    }
}
