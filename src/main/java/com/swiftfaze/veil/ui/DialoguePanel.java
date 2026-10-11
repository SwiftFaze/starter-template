package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.entities.dialogue.Conversation;
import com.swiftfaze.veil.entities.dialogue.DialogueChoice;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import com.swiftfaze.veil.ui.widget.HeaderWidget;
import com.swiftfaze.veil.ui.widget.ListWidget;
import com.swiftfaze.veil.ui.widget.PopupWidget;
import com.swiftfaze.veil.ui.widget.WidgetTheme;
import org.jspecify.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JTextPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.text.StyledDocument;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * The talk popup for one {@link Conversation}: the chat log (every NPC line and picked choice so
 * far) above, and the current node's numbered choices below. Up/Down move the highlight, Enter or a
 * number key picks, and Esc ends the conversation. The world pause is held by
 * {@link DialoguePanelListener}, not here, so every close path releases it.
 */
public final class DialoguePanel extends PopupWidget {

    private static final int PADDING = 10;
    private static final int MAX_CHOICES = 9;
    private static final int LOG_MARGIN = 4;
    /** About three lines of 16pt log text, including its margin. */
    private static final int LOG_MIN_HEIGHT = 72;
    private static final List<ControlsHintBarWidget.Hint> HINTS = List.of(
            new ControlsHintBarWidget.Hint("up/down", "Choose"),
            new ControlsHintBarWidget.Hint("enter", "Pick"),
            new ControlsHintBarWidget.Hint("escape", "End"));

    private final HeaderWidget header = new HeaderWidget("");
    private final JTextPane logText = new JTextPane();
    private final JScrollPane logScroll;
    private final ListWidget<String> choiceList;
    private final JScrollPane choiceScroll;
    private final Consumer<List<ControlsHintBarWidget.Hint>> hintSink;

    private @Nullable Conversation conversation;

    /** @param hintSink receives the controls hints to show while the popup is open */
    public DialoguePanel(Consumer<List<ControlsHintBarWidget.Hint>> hintSink) {
        this.hintSink = hintSink;
        Border outline = BorderFactory.createLineBorder(WidgetTheme.border(), 2);
        Border padding = BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING);
        setBorder(BorderFactory.createCompoundBorder(outline, padding));

        addContent(header);

        // A text pane wraps long lines to the viewport width. Not focusable, so the caret never takes
        // Up/Down away from the choice highlight.
        logText.setEditable(false);
        logText.setFocusable(false);
        logText.setBackground(WidgetTheme.background());
        logText.setMargin(new Insets(LOG_MARGIN, LOG_MARGIN, LOG_MARGIN, LOG_MARGIN));
        // Sized to about three lines, not to its whole text, so the choices keep their full height when
        // they fit. Its minimum keeps those lines visible when the choices need the rest of the box.
        logScroll = ListDetailLayoutUtility.buildScrollPane(logText);
        logScroll.setAlignmentX(LEFT_ALIGNMENT);
        logScroll.setPreferredSize(new Dimension(0, LOG_MIN_HEIGHT));
        logScroll.setMinimumSize(new Dimension(0, LOG_MIN_HEIGHT));
        addContent(logScroll);

        // Scrolls when there are more choices than fit; ListWidget scrolls the highlight into view.
        choiceList = new ListWidget<>(Function.identity());
        choiceList.setWrapAround(false);
        choiceList.setFocusTraversalKeysEnabled(false);
        choiceScroll = ListDetailLayoutUtility.buildScrollPane(choiceList);
        choiceScroll.setAlignmentX(LEFT_ALIGNMENT);
        addContent(choiceScroll);

        bindTalkKeys();
    }

    /** A box along the bottom of the game area, not a full-screen popup, so the player stays visible above it. */
    @Override
    public boolean isFullScreen() {
        return false;
    }

    @Override
    public Placement placement() {
        return Placement.BOTTOM_CENTER;
    }

    /** Opens the popup on a conversation; each conversation starts from its own log. */
    public void show(Conversation next) {
        conversation = next;
        header.setTitle(next.npcName());
        hintSink.accept(HINTS);
        refresh(next);
        open();
    }

    /** The name the popup is headed with: the NPC being talked to. */
    public String title() {
        return header.getTitle();
    }

    /** The chat log's lines, oldest first; empty before the first show. */
    public List<String> logLines() {
        Conversation current = conversation;
        return current == null ? List.of() : current.log().stream().map(Conversation.Line::text).toList();
    }

    /** The text the log pane displays, as opposed to {@link #logLines()}, which is the conversation's. */
    String displayedLog() {
        return logText.getText().replace(System.lineSeparator(), "\n");
    }

    /** Where the log's caret sits; at the end of the text once the log has scrolled to the newest line. */
    int logCaretPosition() {
        return logText.getCaretPosition();
    }

    /** How tall the log section is laid out; it takes whatever the choices leave. */
    int logHeight() {
        return logScroll.getHeight();
    }

    /** How tall the choices section is laid out; its own content's height, pinned to the bottom. */
    int choicesHeight() {
        return choiceScroll.getHeight();
    }

    /** The current node's choices as shown, numbered from 1. */
    public List<String> choiceLabels() {
        Conversation current = conversation;
        return current == null ? List.of() : numberedChoices(current);
    }

    /** The 1-based number of the highlighted choice. */
    public int highlightedChoice() {
        return choiceList.getSelectedIndex() + 1;
    }

    @Override
    protected void onUp() {
        choiceList.moveUp();
    }

    @Override
    protected void onDown() {
        choiceList.moveDown();
    }

    /** Picks the highlighted choice (Enter). */
    void chooseHighlighted() {
        choose(choiceList.getSelectedIndex());
    }

    /** Picks choice {@code number}, counted from 1 (the number keys). A number with no choice is ignored. */
    void chooseNumber(int number) {
        choose(number - 1);
    }

    private void choose(int index) {
        Conversation current = conversation;
        if (current == null || !current.choose(index)) {
            return;
        }
        if (current.isEnded()) {
            dismiss();
        } else {
            refresh(current);
        }
    }

    private void refresh(Conversation current) {
        renderLog(current);
        choiceList.setItems(numberedChoices(current));
        fitChoicesToContent();
    }

    /**
     * Pins the choices to the bottom of the box at their own height, so the log above takes all the
     * spare room. BoxLayout only hands extra height to a child whose maximum exceeds its preferred size.
     */
    private void fitChoicesToContent() {
        Insets insets = choiceScroll.getInsets();
        int height = choiceList.getPreferredSize().height + insets.top + insets.bottom;
        choiceScroll.setPreferredSize(new Dimension(0, height));
        choiceScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        revalidate();
    }

    /** Rewrites the log from the conversation, then scrolls it to the newest line. */
    private void renderLog(Conversation current) {
        StyledDocument document = ChatLogDocument.build(current.log());
        logText.setStyledDocument(document);
        int end = document.getLength();
        // The text is laid out later on the event thread; move the caret after that so the scroll lands.
        SwingUtilities.invokeLater(() -> logText.setCaretPosition(end));
    }

    private static List<String> numberedChoices(Conversation current) {
        List<DialogueChoice> choices = current.currentNode().choices();
        return IntStream.range(0, choices.size())
                .mapToObj(i -> (i + 1) + ". " + choices.get(i).text())
                .toList();
    }

    private void bindTalkKeys() {
        InputMap inputMap = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = getActionMap();

        inputMap.put(Keybindings.MENU_CONFIRM, "dialogue-pick");
        actionMap.put("dialogue-pick", action(this::chooseHighlighted));

        for (int number = 1; number <= MAX_CHOICES; number++) {
            String name = "dialogue-choose-" + number;
            int picked = number;
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_0 + number, 0), name);
            actionMap.put(name, action(() -> chooseNumber(picked)));
        }
    }

    private static AbstractAction action(Runnable task) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                task.run();
            }
        };
    }
}
