package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.player.Stats;
import com.swiftfaze.veil.entities.player.classes.PlayerClass;
import com.swiftfaze.veil.mods.ModLoader;

import java.nio.file.Paths;
import java.util.List;

public class ClassSandboxModel {

    private final List<PlayerClass> classes;

    public ClassSandboxModel() {
        this(ModLoader.load(Paths.get("mods")).getAllPlayerClasses());
    }

    public ClassSandboxModel(List<PlayerClass> classes) {
        this.classes = classes;
    }

    public List<String> classNames() {
        return classes.stream().map(PlayerClass::getName).toList();
    }

    public Stats computedStats(String className) {
        return computedStats(className, 0);
    }

    public Stats computedStats(String className, int level) {
        PlayerClass playerClass = findByName(className);
        Stats stats = new Stats();
        playerClass.applyStatsAtLevel(stats, level);
        return stats;
    }

    public String idFor(String className) {
        return findByName(className).getId();
    }

    public String classNameForId(String id) {
        return findById(id).getName();
    }

    private PlayerClass findByName(String className) {
        return classes.stream()
                .filter(c -> c.getName().equals(className))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown class: " + className));
    }

    private PlayerClass findById(String id) {
        return classes.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown class id: " + id));
    }
}
