package com.swiftfaze.veil.ui.widget;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

/**
 * A bordered, full-width title bar — for a screen that needs a heading (e.g.
 * "Mage" atop a detail view) styled consistently with the rest of the widget
 * framework rather than a bare {@link JLabel}.
 */
public class HeaderWidget extends Widget {
    private static final Font TITLE_FONT = new Font(Font.MONOSPACED, Font.BOLD, 20);
    // Empty space below the header's own visible border box, outside it - so whatever a
    // consumer stacks below (in a BoxLayout) sits with a gap instead of flush against it.
    private static final int BOTTOM_MARGIN = 12;

    private final JLabel titleLabel;

    public HeaderWidget(String title) {
        this.titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setForeground(WidgetTheme.normalText());
        titleLabel.setFont(TITLE_FONT);

        setLayout(new BorderLayout());
        setAlignmentX(LEFT_ALIGNMENT);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, BOTTOM_MARGIN, 0),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(WidgetTheme.border(), 1),
                        BorderFactory.createEmptyBorder(4, 8, 4, 8))));
        add(titleLabel, BorderLayout.CENTER);

        // Fills the full width of whatever BoxLayout container it sits in (matching the
        // convention already used by PatternFieldWidget/TableWidget rows) while keeping its
        // own natural height instead of being stretched vertically.
        setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public String getTitle() {
        return titleLabel.getText();
    }
}
