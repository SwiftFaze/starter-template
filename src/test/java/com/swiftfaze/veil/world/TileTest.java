package com.swiftfaze.veil.world;

import com.swiftfaze.veil.component.DetailTable;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TileTest {

    @Test
    void getNameReturnsId() {
        Tile tile = new Tile("test:grass", '.', Color.GREEN, true);
        assertEquals("test:grass", tile.getName());
    }

    @Test
    void getDetailTablesForWalkableTile() {
        Tile tile = new Tile("test:grass", '.', Color.GREEN, true);

        List<DetailTable> tables = tile.getDetailTables();

        assertEquals(1, tables.size());
        assertEquals("", tables.get(0).label());
        assertEquals(List.of("Field", "Value"), tables.get(0).columnHeaders());
        assertEquals(4, tables.get(0).rows().size());
        assertEquals(List.of("ID", "test:grass"), tables.get(0).rows().get(0));
        assertEquals(List.of("Symbol", "."), tables.get(0).rows().get(1));
        assertEquals(List.of("Walkable", "true"), tables.get(0).rows().get(3));
    }

    @Test
    void getDetailTablesForNonWalkableTile() {
        Tile tile = new Tile("test:wall", '#', Color.DARK_GRAY, false);

        List<DetailTable> tables = tile.getDetailTables();

        assertEquals(1, tables.size());
        assertEquals(4, tables.get(0).rows().size());
        assertEquals(List.of("ID", "test:wall"), tables.get(0).rows().get(0));
        assertEquals(List.of("Symbol", "#"), tables.get(0).rows().get(1));
        assertEquals(List.of("Walkable", "false"), tables.get(0).rows().get(3));
    }

    @Test
    void colorFormattedAsRgb() {
        Tile tile = new Tile("test:red", '*', new Color(255, 100, 50), true);

        List<DetailTable> tables = tile.getDetailTables();

        List<List<String>> rows = tables.get(0).rows();
        List<String> colorRow = rows.stream()
                .filter(row -> row.get(0).equals("Color"))
                .findFirst()
                .orElseThrow();
        assertEquals("rgb(255, 100, 50)", colorRow.get(1));
    }
}
