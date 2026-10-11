package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.game.event.GameEvent;
import com.swiftfaze.veil.game.event.GameEventLog;
import com.swiftfaze.veil.input.Keybindings;
import org.jspecify.annotations.Nullable;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import com.swiftfaze.veil.ui.widget.ListWidget;
import com.swiftfaze.veil.ui.widget.WidgetTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

public final class TitleScreenPanel extends JPanel implements HintAware {
    private static final Logger logger = LoggerFactory.getLogger(TitleScreenPanel.class);
    private static final List<ControlsHintBarWidget.Hint> HINTS = List.of(new ControlsHintBarWidget.Hint("enter", "Select"));

    private final JLabel titleLabel;
    private final ListWidget<String> menuWidget;
    private final Consumer<String> onMenuSelect;
    private final ControlsHintBarWidget hintBar;
    private final GameEventLog eventLog;
    private @Nullable String lastHighlighted;

    public TitleScreenPanel(Consumer<String> onMenuSelect, ControlsHintBarWidget hintBar) {
        this(onMenuSelect, hintBar, GameEventLog.noOp());
    }

    public TitleScreenPanel(Consumer<String> onMenuSelect, ControlsHintBarWidget hintBar, GameEventLog eventLog) {
        this.onMenuSelect = onMenuSelect;
        this.hintBar = hintBar;
        this.eventLog = eventLog;
        setBackground(WidgetTheme.background());
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setFocusable(true);

        // Title
        titleLabel = new JLabel("VEIL");
        titleLabel.setFont(loadTitleFont());
        titleLabel.setForeground(WidgetTheme.normalText());
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Menu
        List<String> menuItems = List.of("Continue", "New", "Load", "Settings", "Exit");
        menuWidget = new ListWidget<>(s -> s);
        menuWidget.setItems(menuItems);
        lastHighlighted = menuWidget.getSelectedItem();
        menuWidget.setOnConfirm(this::handleMenuSelect);
        menuWidget.setOnSelectionChange(this::recordSelectionChange);
        menuWidget.setAlignmentX(Component.CENTER_ALIGNMENT);

        add(Box.createVerticalGlue());
        add(titleLabel);
        add(Box.createVerticalStrut(40));
        add(menuWidget);
        add(Box.createVerticalGlue());

        bindKeys();
        refreshHints();
    }

    private void bindKeys() {
        InputMap inputMap = getInputMap(WHEN_FOCUSED);
        ActionMap actionMap = getActionMap();

        inputMap.put(Keybindings.MENU_UP, "title-up");
        inputMap.put(Keybindings.MENU_DOWN, "title-down");
        inputMap.put(Keybindings.MENU_CONFIRM, "title-confirm");

        actionMap.put("title-up", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveUp();
            }
        });
        actionMap.put("title-down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveDown();
            }
        });
        actionMap.put("title-confirm", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirm();
            }
        });
    }

    private Font loadTitleFont() {
        try {
            InputStream fontStream = getClass().getResourceAsStream("/fonts/DeltaCorpsPriest1.ttf");
            if (fontStream != null) {
                Font customFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                fontStream.close();
                return customFont.deriveFont(48f);
            }
        } catch (FontFormatException | IOException e) {
            logger.warn("Failed to load Delta Corps Priest 1 font, using fallback", e);
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, 48);
    }

    private void handleMenuSelect(String item) {
        onMenuSelect.accept(item);
    }

    // ListWidget reports every highlight refresh, including a move that stays put at a
    // boundary; only a real change of item is a MenuSelectionChanged.
    private void recordSelectionChange(String item) {
        if (item.equals(lastHighlighted)) {
            return;
        }
        lastHighlighted = item;
        eventLog.recordEvent(GameEvent.menuSelectionChanged(item));
    }

    public String getHighlightedMenuItem() {
        return menuWidget.getSelectedItem();
    }

    public void moveUp() {
        menuWidget.moveUp();
    }

    public void moveDown() {
        menuWidget.moveDown();
    }

    public void confirm() {
        handleMenuSelect(menuWidget.getSelectedItem());
    }

    @Override
    public void refreshHints() {
        hintBar.setHints(HINTS);
    }
}
