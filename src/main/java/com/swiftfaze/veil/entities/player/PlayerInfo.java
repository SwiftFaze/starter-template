package com.swiftfaze.veil.entities.player;

import com.swiftfaze.veil.entities.player.classes.PlayerClass;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;

import java.nio.file.Paths;

public class PlayerInfo {
    private static final String DEFAULT_CLASS_ID = CoreClasses.WARRIOR;

    private String firstName;
    private String lastName;
    private Level level;
    private Stats stats;
    private PlayerClass playerClass;
    private final QuestLog questLog;

    public PlayerInfo() {
        this.firstName = "Branor";
        this.lastName = "Hamerfell";
        this.level = new Level();
        this.stats = new Stats();
        this.questLog = new QuestLog();
        ModRegistry mods = ModLoader.load(Paths.get("mods"));
        this.playerClass = mods.getPlayerClass(DEFAULT_CLASS_ID);
        this.playerClass.applyStatsAtLevel(stats, 0);
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public Stats getStats() {
        return stats;
    }

    public void setStats(Stats stats) {
        this.stats = stats;
    }

    public PlayerClass getPlayerClass() {
        return playerClass;
    }

    public void setPlayerClass(PlayerClass playerClass) {
        this.playerClass = playerClass;
        this.playerClass.applyStatsAtLevel(this.stats, 0);
    }

    public QuestLog getQuestLog() {
        return questLog;
    }
}
