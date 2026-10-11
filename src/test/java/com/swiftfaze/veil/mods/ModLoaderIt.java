package com.swiftfaze.veil.mods;

import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.entities.player.Stats;
import com.swiftfaze.veil.entities.player.classes.PlayerClass;
import com.swiftfaze.veil.world.Tile;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test: exercises real disk I/O + JSON parsing against the
 * repo's real mods/ directory, unlike the unit tests.
 * Runs only via {@code mvn verify} (Failsafe), not {@code mvn test}.
 */
class ModLoaderIt {

    @Test
    void loadsCoreSmallHouseBlueprintFromDisk() {
        ModRegistry registry = ModLoader.load(Paths.get("mods"));
        Building building = registry.getBuilding("core:small_house_01");

        assertNotNull(building, "core:small_house_01 should be loaded from mods/core/buildings/");

        List<List<Tile>> blueprint = building.getBlueprint();

        assertEquals(7, blueprint.size(), "fixture is 7 rows tall");
        assertEquals(7, blueprint.get(0).size(), "fixture is 7 columns wide");
        assertEquals(registry.getTile("core:stone"), blueprint.get(0).get(0), "top-left corner is a stone wall");
        assertEquals(registry.getTile("core:wood"), blueprint.get(6).get(3), "the south-wall cell under the door is floor, not a door tile");
        assertEquals(2, building.getObjects().size(), "the house places a door and a starter chest");
        assertEquals(registry.getObject("core:wooden_door").getId(), building.getObjects().get(0).object().getId());
        assertEquals(3, building.getObjects().get(0).x());
        assertEquals(6, building.getObjects().get(0).y());
        assertEquals(registry.getObject("core:starter_chest").getId(), building.getObjects().get(1).object().getId());
        assertEquals(2, building.getObjects().get(1).x());
        assertEquals(5, building.getObjects().get(1).y());
    }

    @Test
    void loadsAllCoreTilesFromDisk() {
        ModRegistry registry = ModLoader.load(Paths.get("mods"));

        Tile grass = registry.getTile("core:grass");
        assertNotNull(grass, "core:grass should be loaded from mods/core/tiles/");
        assertTrue(grass.isWalkable());
        assertEquals('⡐', grass.getSymbol());
    }

    @Test
    void loadsCoreWarriorClassFromDisk() {
        ModRegistry registry = ModLoader.load(Paths.get("mods"));
        PlayerClass warrior = registry.getPlayerClass("core:warrior");

        assertNotNull(warrior, "core:warrior should be loaded from mods/core/classes/");
        assertEquals("Warrior", warrior.getName());

        Stats stats = statsAtLevel(warrior, 0);
        assertEquals(15, stats.getStrength());
        assertEquals(120, stats.getMaxHp());
    }

    @Test
    void loadsCoreMageClassFromDisk() {
        ModRegistry registry = ModLoader.load(Paths.get("mods"));
        PlayerClass mage = registry.getPlayerClass("core:mage");

        assertNotNull(mage, "core:mage should be loaded from mods/core/classes/");
        assertEquals("Mage", mage.getName());

        Stats stats = statsAtLevel(mage, 0);
        assertEquals(16, stats.getIntelligence());
        assertEquals(70, stats.getMaxHp());
        assertEquals(100, stats.getMaxMana());
    }

    @Test
    void loadsCoreIronSwordFromDisk() {
        ModRegistry registry = ModLoader.load(Paths.get("mods"));
        Item ironSword = registry.getItem("core:iron_sword");

        assertNotNull(ironSword, "core:iron_sword should be loaded from mods/core/items/");
        assertEquals("Iron Sword", ironSword.getName());
        assertEquals(4, ironSword.getBaseDamage().min());
        assertEquals(9, ironSword.getBaseDamage().max());
        assertEquals(1, ironSword.getEffects().size());
        assertEquals("strength", ironSword.getEffects().get(0).stat());
    }

    @Test
    void loadsCoreDefaultThemeFromDisk() {
        ModRegistry registry = ModLoader.load(Paths.get("mods"));
        WidgetColorTheme theme = registry.getTheme("core:default");

        assertNotNull(theme, "core:default should be loaded from mods/core/themes/default.json");
        for (String key : WidgetColorTheme.REQUIRED_KEYS) {
            assertNotNull(theme.color(key), "expected theme to define color '" + key + "'");
        }
        assertEquals(new Color(0, 0, 0), theme.color("BACKGROUND"));
    }

    private static Stats statsAtLevel(PlayerClass playerClass, int level) {
        Stats stats = new Stats();
        playerClass.applyStatsAtLevel(stats, level);
        return stats;
    }
}
