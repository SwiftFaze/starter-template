package com.swiftfaze.veil.entities.player.classes;

import org.jspecify.annotations.Nullable;
import com.swiftfaze.veil.component.DetailTable;
import com.swiftfaze.veil.component.Inspectable;
import com.swiftfaze.veil.entities.player.Stats;
import com.swiftfaze.veil.mods.CalcExpressionParser;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class PlayerClass implements Inspectable {

    public record StatCurve(int base, @Nullable String growthCalc) {
    }

    private static final Map<String, BiConsumer<Stats, Integer>> ATTRIBUTE_SETTERS = Map.of(
            "strength", Stats::setStrength,
            "dexterity", Stats::setDexterity,
            "constitution", Stats::setConstitution,
            "intelligence", Stats::setIntelligence,
            "wisdom", Stats::setWisdom,
            "luck", Stats::setLuck
    );

    private final String id;
    private final String name;
    private final Map<String, StatCurve> statsByName;

    public PlayerClass(String id, String name, Map<String, StatCurve> statsByName) {
        this.id = id;
        this.name = name;
        this.statsByName = statsByName;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void applyStatsAtLevel(Stats stats, int level) {
        for (Map.Entry<String, BiConsumer<Stats, Integer>> entry : ATTRIBUTE_SETTERS.entrySet()) {
            entry.getValue().accept(stats, computeStat(entry.getKey(), level));
        }

        int maxHp = computeStat("maxHp", level);
        stats.setMaxHp(maxHp);
        stats.setCurrentHp(maxHp);

        int maxMana = computeStat("maxMana", level);
        stats.setMaxMana(maxMana);
        stats.setCurrentMana(maxMana);
    }

    private int computeStat(String statName, int level) {
        StatCurve curve = statsByName.getOrDefault(statName, new StatCurve(0, null));
        double growth = curve.growthCalc() == null ? 0 : CalcExpressionParser.evaluate(curve.growthCalc(), level);
        return Math.toIntExact(Math.round(curve.base() + growth));
    }

    @Override
    public List<DetailTable> getDetailTables() {
        List<List<String>> rows = List.of(
                List.of("ID", id),
                List.of("Name", name)
        );
        return List.of(new DetailTable("", List.of("Field", "Value"), rows));
    }
}
