package com.swiftfaze.veil.entities.quests;

import com.swiftfaze.veil.component.DetailTable;
import com.swiftfaze.veil.component.Inspectable;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

public class Quest implements Inspectable {

    public record Objective(String type, @Nullable String target, int count) {
    }

    public record Reward(String type, @Nullable String id, @Nullable Integer count, @Nullable String calc) {
    }

    private final String id;
    private final String name;
    private final Objective objective;
    private final List<Reward> rewards;

    public Quest(String id, String name, Objective objective, List<Reward> rewards) {
        this.id = id;
        this.name = name;
        this.objective = objective;
        this.rewards = rewards;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public Objective getObjective() {
        return objective;
    }

    public List<Reward> getRewards() {
        return rewards;
    }

    @Override
    public List<DetailTable> getDetailTables() {
        List<DetailTable> tables = new ArrayList<>();
        tables.add(fieldsTable());
        if (!rewards.isEmpty()) {
            tables.add(rewardsTable());
        }
        return tables;
    }

    public DetailTable fieldsTable() {
        List<List<String>> fieldRows = new ArrayList<>(List.of(
                List.of("ID", id),
                List.of("Name", name),
                List.of("Objective Type", objective.type),
                List.of("Objective Target", objective.target != null ? objective.target : "-"),
                List.of("Objective Count", String.valueOf(objective.count))
        ));
        return new DetailTable("", List.of("Field", "Value"), fieldRows);
    }

    public DetailTable rewardsTable() {
        List<List<String>> rewardRows = rewards.stream()
                .map(r -> List.of(
                        r.type(),
                        r.id() != null ? r.id() : "-",
                        r.count() != null ? String.valueOf(r.count()) : "-",
                        r.calc() != null ? r.calc() : "-"
                ))
                .toList();
        return new DetailTable("Rewards:", List.of("Type", "ID", "Count", "Calc"), rewardRows);
    }
}
