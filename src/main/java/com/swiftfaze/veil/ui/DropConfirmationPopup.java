package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.ui.widget.CompactPopupWidget;
import com.swiftfaze.veil.ui.widget.RadioGroupWidget;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JTextPane;
import java.awt.Component;
import java.util.List;

public class DropConfirmationPopup extends CompactPopupWidget {
    private static final String DEFAULT_ITEM_NAME = "this item";
    private static final String QUESTION_PREFIX = "Are you sure you want to drop ";
    private static final String QUESTION_SUFFIX = "?";

    private final RadioGroupWidget<String> choice;
    private final JTextPane questionLabel;

    public DropConfirmationPopup() {
        super("Drop Item");

        questionLabel = createBodyLabel();
        setQuestionText(DEFAULT_ITEM_NAME);
        addContent(questionLabel);

        addContent((JComponent) Box.createVerticalGlue());

        choice = new RadioGroupWidget<>(s -> s, /* horizontal= */ true);
        choice.setFillWidth(true);
        choice.setOptions(List.of("No", "Yes"));
        choice.setOnConfirm(selected -> dismiss());
        choice.setAlignmentX(Component.CENTER_ALIGNMENT);
        choice.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        addContent(choice);
    }

    @Override
    public void open() {
        open(DEFAULT_ITEM_NAME);
    }

    /**
     * Opens the popup naming the specific item being dropped, e.g. "Drop Iron Sword" as the
     * title and "Are you sure you want to drop Iron Sword?" as the body — the item name isn't
     * known until an item is actually selected in the inventory, so it can't be baked into the
     * constructor like the rest of this dialog's static text.
     */
    public void open(String itemName) {
        setTitleText("Drop " + itemName);
        setQuestionText(itemName);
        super.open();
        choice.resetSelection();
        choice.requestFocusInWindow();
    }

    public RadioGroupWidget<String> getChoiceWidget() {
        return choice;
    }

    private void setQuestionText(String itemName) {
        String text = QUESTION_PREFIX + itemName + QUESTION_SUFFIX;
        setBodyText(questionLabel, text, QUESTION_PREFIX.length(), QUESTION_PREFIX.length() + itemName.length());
    }
}
