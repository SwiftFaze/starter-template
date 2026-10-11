package com.swiftfaze.veil.entities.items;

import com.swiftfaze.veil.component.DetailTable;
import com.swiftfaze.veil.component.Inspectable;
import java.util.ArrayList;
import java.util.List;

public class Item implements Inspectable {

    public record BaseDamage(int min, int max) {
    }

    public record Effect(String type, String stat, String calc) {
    }

    public record ItemAttributes(char glyph, String type, String slot, BaseDamage baseDamage, List<Effect> effects) {
    }

    private final String id;
    private final String name;
    private final char glyph;
    private final String type;
    private final String slot;
    private final BaseDamage baseDamage;
    private final List<Effect> effects;

    public Item(String id, String name, ItemAttributes attributes) {
        this.id = id;
        this.name = name;
        this.glyph = attributes.glyph();
        this.type = attributes.type();
        this.slot = attributes.slot();
        this.baseDamage = attributes.baseDamage();
        this.effects = attributes.effects();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public char getGlyph() {
        return glyph;
    }

    public String getType() {
        return type;
    }

    public String getSlot() {
        return slot;
    }

    public BaseDamage getBaseDamage() {
        return baseDamage;
    }

    public List<Effect> getEffects() {
        return effects;
    }

    public Item withBaseDamage(int minDamage, int maxDamage) {
        BaseDamage newBaseDamage = new BaseDamage(minDamage, maxDamage);
        return new Item(id, name, new ItemAttributes(glyph, type, slot, newBaseDamage, effects));
    }

    @Override
    public List<DetailTable> getDetailTables() {
        List<List<String>> fieldRows = new ArrayList<>(List.of(
                List.of("ID", id),
                List.of("Name", name),
                List.of("Glyph", String.valueOf(glyph)),
                List.of("Type", type),
                List.of("Slot", slot)
        ));
        if (baseDamage.max() > 0) {
            fieldRows.add(List.of("Base Damage (Min)", String.valueOf(baseDamage.min())));
            fieldRows.add(List.of("Base Damage (Max)", String.valueOf(baseDamage.max())));
        }
        List<DetailTable> tables = new ArrayList<>();
        tables.add(new DetailTable("", List.of("Field", "Value"), fieldRows));
        if (!effects.isEmpty()) {
            List<List<String>> effectRows = effects.stream()
                    .map(e -> List.of(e.type(), e.stat(), e.calc()))
                    .toList();
            tables.add(new DetailTable("Effects:", List.of("Type", "Stat", "Calc"), effectRows));
        }
        return tables;
    }
}
