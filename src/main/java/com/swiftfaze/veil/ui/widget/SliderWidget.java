package com.swiftfaze.veil.ui.widget;

import com.swiftfaze.veil.input.Keybindings;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;

public class SliderWidget extends Widget {
    private static final int BAR_LENGTH = 10;

    private final int min;
    private final int max;
    private final int step;
    private int value;
    private final JLabel display;

    public SliderWidget(int min, int max, int step, int initialValue) {
        this.min = min;
        this.max = max;
        this.step = step;
        this.value = Math.max(min, Math.min(max, initialValue));

        setLayout(new BorderLayout());
        display = new JLabel(renderBar());
        display.setForeground(WidgetTheme.normalText());
        display.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        add(display, BorderLayout.CENTER);

        bindKeys();
    }

    public int getValue() {
        return value;
    }

    public String getDisplayText() {
        return renderBar();
    }

    public void moveRight() {
        int newValue = value + step;
        if (newValue <= max) {
            value = newValue;
            updateDisplay();
        }
    }

    public void moveLeft() {
        int newValue = value - step;
        if (newValue >= min) {
            value = newValue;
            updateDisplay();
        }
    }

    private void bindKeys() {
        InputMap inputMap = getInputMap(WHEN_FOCUSED);
        ActionMap actionMap = getActionMap();

        inputMap.put(Keybindings.MENU_LEFT, "slider-left");
        inputMap.put(Keybindings.MENU_RIGHT, "slider-right");

        actionMap.put("slider-left", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveLeft();
            }
        });
        actionMap.put("slider-right", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveRight();
            }
        });
    }

    private void updateDisplay() {
        display.setText(renderBar());
    }

    private String renderBar() {
        int range = max - min;
        int filled = range == 0 ? BAR_LENGTH : (value - min) * BAR_LENGTH / range;
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < BAR_LENGTH; i++) {
            bar.append(i < filled ? '=' : '-');
        }
        return "[" + bar + "] " + value;
    }
}
