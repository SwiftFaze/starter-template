package com.swiftfaze.veil.world;

import com.swiftfaze.veil.component.DetailTable;
import com.swiftfaze.veil.component.Inspectable;
import java.awt.Color;
import java.util.List;

public final class Tile implements Inspectable {
    private final String id;
    private final char symbol;
    private final Color color;
    private final boolean walkable;

    public Tile(String id, char symbol, Color color, boolean walkable) {
        this.id = id;
        this.symbol = symbol;
        this.color = color;
        this.walkable = walkable;
    }

    @Override
    public String getId() {
        return id;
    }

    public char getSymbol() {
        return symbol;
    }

    public Color getColor() {
        return color;
    }

    public boolean isWalkable() {
        return walkable;
    }

    @Override
    public String getName() {
        return id;
    }

    @Override
    public List<DetailTable> getDetailTables() {
        List<List<String>> rows = List.of(
                List.of("ID", id),
                List.of("Symbol", String.valueOf(symbol)),
                List.of("Color", "rgb(" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ")"),
                List.of("Walkable", String.valueOf(walkable))
        );
        return List.of(new DetailTable("", List.of("Field", "Value"), rows));
    }
}
