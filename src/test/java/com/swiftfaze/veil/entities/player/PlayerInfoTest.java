package com.swiftfaze.veil.entities.player;

import com.swiftfaze.veil.entities.player.classes.PlayerClass;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PlayerInfoTest {

    @Test
    void defaultFieldsAreSetOnConstruction() {
        PlayerInfo playerInfo = new PlayerInfo();

        assertEquals("Branor", playerInfo.getFirstName());
        assertEquals("Hamerfell", playerInfo.getLastName());
        assertNotNull(playerInfo.getLevel());
    }

    @Test
    void firstAndLastNameAreMutable() {
        PlayerInfo playerInfo = new PlayerInfo();

        playerInfo.setFirstName("Aria");
        playerInfo.setLastName("Nightwind");

        assertEquals("Aria", playerInfo.getFirstName());
        assertEquals("Nightwind", playerInfo.getLastName());
    }

    @Test
    void levelIsMutable() {
        PlayerInfo playerInfo = new PlayerInfo();
        Level newLevel = new Level();
        newLevel.setCurrentLevel(3);

        playerInfo.setLevel(newLevel);

        assertEquals(newLevel, playerInfo.getLevel());
        assertEquals(3, playerInfo.getLevel().getCurrentLevel());
    }

    @Test
    void statsCanBeReplacedDirectly() {
        PlayerInfo playerInfo = new PlayerInfo();
        Stats newStats = new Stats();

        playerInfo.setStats(newStats);

        assertEquals(newStats, playerInfo.getStats());
    }

    @Test
    void settingPlayerClassReappliesBaseStats() {
        PlayerInfo playerInfo = new PlayerInfo();
        assertEquals(120, playerInfo.getStats().getMaxHp());

        ModRegistry mods = ModLoader.load(Paths.get("mods"));
        PlayerClass mage = mods.getPlayerClass(CoreClasses.MAGE);
        playerInfo.setPlayerClass(mage);

        assertEquals("Mage", playerInfo.getPlayerClass().getName());
        assertEquals(70, playerInfo.getStats().getMaxHp());
    }

    @Test
    void questLogIsNeverNull() {
        PlayerInfo playerInfo = new PlayerInfo();

        assertNotNull(playerInfo.getQuestLog());
    }
}
