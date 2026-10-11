package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.entities.player.PlayerInfo;
import com.swiftfaze.veil.entities.player.Stats;
import com.swiftfaze.veil.entities.player.classes.PlayerClass;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.ui.widget.HeaderWidget;
import com.swiftfaze.veil.ui.widget.TableWidget;
import com.swiftfaze.veil.ui.widget.WidgetTheme;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Live editable stats for the running player. Supports arming a row and
 * adjusting it with Left/Right, cycling Class with Left/Right, and live
 * recomputation of Attack Power/Defense as their underlying attributes change.
 * Also supports editing the running player's X and Y position rows.
 */
public final class PlayerDetailPanel extends JPanel {

    private static final int EDITABLE_CLASS = 0;
    private static final int EDITABLE_STRENGTH = 1;
    private static final int EDITABLE_DEXTERITY = 2;
    private static final int EDITABLE_CONSTITUTION = 3;
    private static final int EDITABLE_INTELLIGENCE = 4;
    private static final int EDITABLE_WISDOM = 5;
    private static final int EDITABLE_LUCK = 6;
    private static final int EDITABLE_MAX_HP = 7;
    private static final int EDITABLE_MAX_MANA = 8;
    private static final int EDITABLE_CURRENT_HP = 9;
    private static final int EDITABLE_CURRENT_MANA = 10;
    private static final int EDITABLE_X = 11;
    private static final int EDITABLE_Y = 12;
    private static final int READONLY_ATTACK_POWER = 13;
    private static final int READONLY_DEFENSE = 14;

    private final Stats stats;
    private final PlayerInfo playerInfo;
    private final IntSupplier playerX;
    private final IntSupplier playerY;
    private final BiConsumer<Integer, Integer> positionSetter;
    private final List<PlayerClass> playerClasses;
    private final List<Supplier<String[]>> rowDataSuppliers;
    private final HeaderWidget header;
    private final TableWidget<List<String>> statsTable;

    private int armedRowIndex = -1;
    private int classListIndex = 0;

    public PlayerDetailPanel(Player player) {
        this.playerX = player::getX;
        this.playerY = player::getY;
        this.positionSetter = player::setPosition;
        this.stats = player.getPlayerInfo().getStats();
        this.playerInfo = player.getPlayerInfo();
        this.playerClasses = ModLoader.load(Paths.get("mods")).getAllPlayerClasses();

        // Find current class index
        String currentClassName = playerInfo.getPlayerClass().getId();
        this.classListIndex = playerClasses.stream()
            .map(PlayerClass::getId)
            .toList()
            .indexOf(currentClassName);
        if (classListIndex < 0) {
            classListIndex = 0;
        }

        this.rowDataSuppliers = buildRowDataSuppliers();
        this.header = new HeaderWidget("Player");
        this.statsTable = TableWidget.ofRows(
            List.of("Field", "Value"),
            buildRows()
        );

        setBackground(WidgetTheme.background());
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setFocusable(true);

        add(header);
        add(statsTable);

        bindKeys();
    }

    public TableWidget<List<String>> getStatsTable() {
        return statsTable;
    }

    /**
     * Refreshes every row's displayed text from the live {@link Stats}/
     * {@link PlayerInfo} object — for a caller that mutated those directly
     * (bypassing this panel's own arm/adjust interaction) and needs the
     * table's cached cell text to catch up, e.g. Cucumber step definitions
     * setting up a scenario's initial values.
     */
    public void refreshAllRows() {
        updateAllRows();
    }

    private List<Supplier<String[]>> buildRowDataSuppliers() {
        return List.of(
            () -> rowData("Class", playerInfo.getPlayerClass().getName()),
            () -> rowData("Strength", stats.getStrength()),
            () -> rowData("Dexterity", stats.getDexterity()),
            () -> rowData("Constitution", stats.getConstitution()),
            () -> rowData("Intelligence", stats.getIntelligence()),
            () -> rowData("Wisdom", stats.getWisdom()),
            () -> rowData("Luck", stats.getLuck()),
            () -> rowData("Max HP", stats.getMaxHp()),
            () -> rowData("Max Mana", stats.getMaxMana()),
            () -> rowData("Current HP", stats.getCurrentHp()),
            () -> rowData("Current Mana", stats.getCurrentMana()),
            () -> rowData("X", playerX.getAsInt()),
            () -> rowData("Y", playerY.getAsInt()),
            () -> rowData("Attack Power", stats.getAttackPower()),
            () -> rowData("Defense", stats.getDefense())
        );
    }

    private List<List<String>> buildRows() {
        return rowDataSuppliers.stream()
            .map(supplier -> List.of(supplier.get()))
            .toList();
    }

    private void bindKeys() {
        InputMap inputMap = getInputMap(WHEN_FOCUSED);
        ActionMap actionMap = getActionMap();

        inputMap.put(Keybindings.MENU_UP, Keybindings.ACTION_MENU_UP);
        inputMap.put(Keybindings.MENU_DOWN, Keybindings.ACTION_MENU_DOWN);
        inputMap.put(Keybindings.MENU_LEFT, Keybindings.ACTION_MENU_LEFT);
        inputMap.put(Keybindings.MENU_RIGHT, Keybindings.ACTION_MENU_RIGHT);
        inputMap.put(Keybindings.MENU_CONFIRM, Keybindings.ACTION_MENU_CONFIRM);
        // MENU_CANCEL (Escape) is deliberately NOT bound here at rest - see applyArmedStyle()/
        // disarm(), which add/remove it only while a row is armed. Binding it unconditionally
        // would swallow every Escape press at this panel's own WHEN_FOCUSED level even when
        // unarmed, since a WHEN_FOCUSED match wins over DevConsolePanel's providerContainer
        // WHEN_ANCESTOR_OF_FOCUSED_COMPONENT back-navigation binding regardless of what the
        // matched action does - so an always-present no-op handler would silently block
        // returning to the search view instead of letting Escape bubble up to it.

        actionMap.put(Keybindings.ACTION_MENU_UP, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleUp();
            }
        });

        actionMap.put(Keybindings.ACTION_MENU_DOWN, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleDown();
            }
        });

        actionMap.put(Keybindings.ACTION_MENU_LEFT, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLeft();
            }
        });

        actionMap.put(Keybindings.ACTION_MENU_RIGHT, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleRight();
            }
        });

        actionMap.put(Keybindings.ACTION_MENU_CONFIRM, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleEnter();
            }
        });

        actionMap.put(Keybindings.ACTION_MENU_CANCEL, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleEscape();
            }
        });
    }

    private void handleUp() {
        if (armedRowIndex != -1) {
            return;
        }
        statsTable.moveUp();
    }

    private void handleDown() {
        if (armedRowIndex != -1) {
            return;
        }
        statsTable.moveDown();
    }

    private void handleLeft() {
        if (armedRowIndex == -1) {
            return;
        }

        if (armedRowIndex == EDITABLE_CLASS) {
            cycleClassBackward();
        } else {
            adjustStatValue(armedRowIndex, -1);
        }
    }

    private void handleRight() {
        if (armedRowIndex == -1) {
            return;
        }

        if (armedRowIndex == EDITABLE_CLASS) {
            cycleClassForward();
        } else {
            adjustStatValue(armedRowIndex, 1);
        }
    }

    private void handleEnter() {
        int selectedRowIndex = statsTable.getSelectedRowIndex();
        if (selectedRowIndex == READONLY_ATTACK_POWER || selectedRowIndex == READONLY_DEFENSE) {
            return;
        }

        if (armedRowIndex == -1) {
            armedRowIndex = selectedRowIndex;
            applyArmedStyle();
        } else {
            disarm();
        }
    }

    private void handleEscape() {
        if (armedRowIndex != -1) {
            disarm();
        }
    }

    private void adjustStatValue(int rowIndex, int delta) {
        applyAdjustment(rowIndex, delta);

        updateRow(rowIndex);
        if (rowIndex >= EDITABLE_STRENGTH && rowIndex <= EDITABLE_CONSTITUTION) {
            updateDerivedStats();
        }
    }

    private void applyAdjustment(int rowIndex, int delta) {
        if (rowIndex >= EDITABLE_STRENGTH && rowIndex <= EDITABLE_LUCK) {
            adjustAttribute(rowIndex, delta);
        } else if (rowIndex == EDITABLE_X) {
            adjustPositionX(delta);
        } else if (rowIndex == EDITABLE_Y) {
            adjustPositionY(delta);
        } else {
            adjustVital(rowIndex, delta);
        }
    }

    private void adjustVital(int rowIndex, int delta) {
        switch (rowIndex) {
            case EDITABLE_MAX_HP -> adjustMaxHp(delta);
            case EDITABLE_MAX_MANA -> adjustMaxMana(delta);
            case EDITABLE_CURRENT_HP -> adjustCurrentHp(delta);
            case EDITABLE_CURRENT_MANA -> adjustCurrentMana(delta);
            default -> { }
        }
    }

    private void adjustAttribute(int rowIndex, int delta) {
        int current = getAttributeValue(rowIndex);
        int newValue = Math.max(0, current + delta);
        setAttributeValue(rowIndex, newValue);
    }

    private int getAttributeValue(int rowIndex) {
        return switch (rowIndex) {
            case EDITABLE_STRENGTH -> stats.getStrength();
            case EDITABLE_DEXTERITY -> stats.getDexterity();
            case EDITABLE_CONSTITUTION -> stats.getConstitution();
            case EDITABLE_INTELLIGENCE -> stats.getIntelligence();
            case EDITABLE_WISDOM -> stats.getWisdom();
            case EDITABLE_LUCK -> stats.getLuck();
            default -> 0;
        };
    }

    private void setAttributeValue(int rowIndex, int value) {
        switch (rowIndex) {
            case EDITABLE_STRENGTH -> stats.setStrength(value);
            case EDITABLE_DEXTERITY -> stats.setDexterity(value);
            case EDITABLE_CONSTITUTION -> stats.setConstitution(value);
            case EDITABLE_INTELLIGENCE -> stats.setIntelligence(value);
            case EDITABLE_WISDOM -> stats.setWisdom(value);
            case EDITABLE_LUCK -> stats.setLuck(value);
            default -> {
                // Not an attribute row (class, HP and mana rows are adjusted elsewhere)
            }
        }
    }

    private void adjustMaxHp(int delta) {
        int newValue = Math.max(1, stats.getMaxHp() + delta);
        stats.setMaxHp(newValue);
        if (stats.getCurrentHp() > newValue) {
            stats.setCurrentHp(newValue);
            updateRow(EDITABLE_CURRENT_HP);
        }
    }

    private void adjustMaxMana(int delta) {
        int newValue = Math.max(1, stats.getMaxMana() + delta);
        stats.setMaxMana(newValue);
        if (stats.getCurrentMana() > newValue) {
            stats.setCurrentMana(newValue);
            updateRow(EDITABLE_CURRENT_MANA);
        }
    }

    private void adjustCurrentHp(int delta) {
        int newValue = Math.max(0, stats.getCurrentHp() + delta);
        stats.setCurrentHp(newValue);
    }

    private void adjustCurrentMana(int delta) {
        int newValue = Math.max(0, stats.getCurrentMana() + delta);
        stats.setCurrentMana(newValue);
    }

    private void adjustPositionX(int delta) {
        int newX = Math.max(0, playerX.getAsInt() + delta);
        positionSetter.accept(newX, playerY.getAsInt());
    }

    private void adjustPositionY(int delta) {
        int newY = Math.max(0, playerY.getAsInt() + delta);
        positionSetter.accept(playerX.getAsInt(), newY);
    }

    private void updateDerivedStats() {
        updateRow(READONLY_ATTACK_POWER);
        updateRow(READONLY_DEFENSE);
    }

    private void cycleClassForward() {
        classListIndex = (classListIndex + 1) % playerClasses.size();
        applyClassChange();
    }

    private void cycleClassBackward() {
        classListIndex = (classListIndex - 1 + playerClasses.size()) % playerClasses.size();
        applyClassChange();
    }

    private void applyClassChange() {
        PlayerClass newClass = playerClasses.get(classListIndex);
        playerInfo.setPlayerClass(newClass);
        updateAllRows();
    }

    private void updateRow(int rowIndex) {
        String[] row = getRowData(rowIndex);
        statsTable.updateRow(rowIndex, List.of(row[0], row[1]));
    }

    private String[] getRowData(int rowIndex) {
        return rowDataSuppliers.get(rowIndex).get();
    }

    private static String[] rowData(String fieldName, int value) {
        return new String[]{fieldName, String.valueOf(value)};
    }

    private static String[] rowData(String fieldName, String value) {
        return new String[]{fieldName, value};
    }

    private void updateAllRows() {
        for (int i = 0; i <= READONLY_DEFENSE; i++) {
            updateRow(i);
        }
    }

    private void applyArmedStyle() {
        statsTable.setSelectedRowAccentColor(WidgetTheme.validHighlight());
        statsTable.setOtherRowsDimmed(true);
        getInputMap(WHEN_FOCUSED).put(Keybindings.MENU_CANCEL, Keybindings.ACTION_MENU_CANCEL);
    }

    private void disarm() {
        armedRowIndex = -1;
        statsTable.setSelectedRowAccentColor(null);
        statsTable.setOtherRowsDimmed(false);
        getInputMap(WHEN_FOCUSED).remove(Keybindings.MENU_CANCEL);
    }
}
