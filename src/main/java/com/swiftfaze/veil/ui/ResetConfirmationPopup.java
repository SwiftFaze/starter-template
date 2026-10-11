package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.ui.widget.CompactPopupWidget;
import com.swiftfaze.veil.ui.widget.RadioGroupWidget;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JTextPane;
import java.awt.Component;
import java.util.List;

/**
 * A Yes/No confirmation dialog for settings actions (Reset to Defaults,
 * Discard Changes, etc.). Extends CompactPopupWidget to provide a smaller,
 * centered presentation with a title bar. Uses RadioGroupWidget for the
 * choice, with "No" as the default (safe-default convention matching
 * DropConfirmationPopup). Supports a customizable onYes callback that only
 * fires when "Yes" is actually chosen.
 */
public class ResetConfirmationPopup extends CompactPopupWidget {
    private final RadioGroupWidget<String> choice;
    private final String title;
    private final String questionText;
    private Runnable onYes = () -> {};

    public ResetConfirmationPopup() {
        this("Confirm Reset", "Reset all settings to their defaults?");
    }

    public ResetConfirmationPopup(String title, String questionText) {
        super(title);
        this.title = title;
        this.questionText = questionText;

        JTextPane questionLabel = createBodyLabel();
        setBodyText(questionLabel, questionText);
        addContent(questionLabel);

        addContent((JComponent) Box.createVerticalGlue());

        choice = new RadioGroupWidget<>(s -> s, /* horizontal= */ true);
        choice.setFillWidth(true);
        choice.setOptions(List.of("No", "Yes"));
        choice.setOnConfirm(selected -> {
            if ("Yes".equals(selected)) {
                onYes.run();
            }
            dismiss();
        });
        choice.setAlignmentX(Component.CENTER_ALIGNMENT);
        choice.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        addContent(choice);
    }

    public void setOnYes(Runnable onYes) {
        this.onYes = onYes;
    }

    @Override
    public void open() {
        super.open();
        choice.resetSelection();
        choice.requestFocusInWindow();
    }

    public RadioGroupWidget<String> getChoiceWidget() {
        return choice;
    }

    public String getTitle() {
        return title;
    }

    public String getQuestionText() {
        return questionText;
    }
}
