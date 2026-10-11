package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.player.Stats;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.widget.HeaderWidget;
import com.swiftfaze.veil.ui.widget.TableWidget;
import com.swiftfaze.veil.ui.widget.WidgetTheme;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A single class's full computed stats, pre-selected to exactly the class a
 * dev-console search opened — no browsable list of every other class, unlike
 * {@link ClassSandboxPanel} (which stays as-is: it's a proof case the
 * pre-existing ui-component-framework.feature depends on, not part of the
 * dev console anymore).
 */
public class ClassDetailPanel extends JPanel {

    private static final List<Integer> SAMPLED_LEVELS = List.of(0, 5, 10, 15, 20);
    private final HeaderWidget header;
    private final TableWidget<List<String>> statsTable;

    public ClassDetailPanel(ClassSandboxModel model, String className) {
        this.header = new HeaderWidget(className);
        this.statsTable = TableWidget.ofRows(buildColumnHeaders(), detailRows(model, className));

        super.setBackground(WidgetTheme.background());
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setFocusable(true);

        add(header);
        add(statsTable);

        bindKeys();
    }

    public HeaderWidget getHeader() {
        return header;
    }

    public TableWidget<List<String>> getStatsTable() {
        return statsTable;
    }

    private void bindKeys() {
        InputMap inputMap = getInputMap(WHEN_FOCUSED);
        ActionMap actionMap = getActionMap();

        inputMap.put(Keybindings.MENU_UP, Keybindings.ACTION_MENU_UP);
        inputMap.put(Keybindings.MENU_DOWN, Keybindings.ACTION_MENU_DOWN);

        actionMap.put(Keybindings.ACTION_MENU_UP, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                statsTable.moveUp();
            }
        });

        actionMap.put(Keybindings.ACTION_MENU_DOWN, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                statsTable.moveDown();
            }
        });
    }

    private static List<String> buildColumnHeaders() {
        List<String> headers = new ArrayList<>(SAMPLED_LEVELS.size() + 1);
        headers.add("Stat");
        for (int level : SAMPLED_LEVELS) {
            headers.add("Lv " + level);
        }
        return headers;
    }

    private static List<List<String>> detailRows(ClassSandboxModel model, String className) {
        List<Stats> statsPerLevel = new ArrayList<>(SAMPLED_LEVELS.size());
        for (int level : SAMPLED_LEVELS) {
            statsPerLevel.add(model.computedStats(className, level));
        }
        return List.of(
                buildRow(statsPerLevel, "Attack Power", Stats::getAttackPower),
                buildRow(statsPerLevel, "Defense", Stats::getDefense),
                buildRow(statsPerLevel, "Max HP", Stats::getMaxHp),
                buildRow(statsPerLevel, "Max Mana", Stats::getMaxMana),
                buildRow(statsPerLevel, "Strength", Stats::getStrength),
                buildRow(statsPerLevel, "Dexterity", Stats::getDexterity),
                buildRow(statsPerLevel, "Constitution", Stats::getConstitution),
                buildRow(statsPerLevel, "Intelligence", Stats::getIntelligence),
                buildRow(statsPerLevel, "Wisdom", Stats::getWisdom),
                buildRow(statsPerLevel, "Luck", Stats::getLuck)
        );
    }

    private static List<String> buildRow(Iterable<Stats> statsPerLevel, String statName,
                                         Function<Stats, Integer> statGetter) {
        List<String> row = new ArrayList<>(SAMPLED_LEVELS.size() + 1);
        row.add(statName);
        for (Stats stats : statsPerLevel) {
            row.add(String.valueOf(statGetter.apply(stats)));
        }
        return row;
    }
}
