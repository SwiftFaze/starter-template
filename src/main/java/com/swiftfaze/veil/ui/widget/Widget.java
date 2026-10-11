package com.swiftfaze.veil.ui.widget;

import javax.swing.JPanel;

public abstract class Widget extends JPanel {

    public Widget() {
        super.setBackground(WidgetTheme.background());
        setFocusable(true);
    }
}
