package com.swiftfaze.veil.sandbox;

import org.jspecify.annotations.Nullable;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.ListDetailLayoutUtility;
import com.swiftfaze.veil.ui.widget.HeaderWidget;
import com.swiftfaze.veil.ui.widget.PatternFieldWidget;
import com.swiftfaze.veil.ui.widget.SuggestionOverlayWidget;
import com.swiftfaze.veil.ui.widget.TranscriptWidget;
import com.swiftfaze.veil.ui.widget.WidgetTheme;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Random;

/**
 * Top-level dev-console shell: an append-only log transcript of every typed command and its
 * output, always shown on launch. Typing `search <term>` and pressing Enter filters entries and
 * prints a numbered result table into the transcript; typing `edit <namespace:id>` and
 * pressing Enter opens that entry's detail panel in place of the transcript. `set`/`add`/
 * `subtract` mutate a field and write a transcript line. `reload` asks every provider to
 * refresh from mods/ and rebuilds the entry list. Escape returns from a detail panel to
 * the transcript.
 */
public final class DevConsolePanel extends JPanel {

    private static final String SEARCH_CARD = "search";
    private static final String PROVIDER_CARD = "provider";
    private static final Dimension DEFAULT_SIZE = new Dimension(820, 600);
    private static final String TITLE = "Dev Console";
    // ui-styling.md's "component gap" - the fixed spacing between two sibling components.
    private static final int COMPONENT_GAP_PX = 8;

    private final CardLayout cardLayout;
    private final JPanel cards;
    private final PatternFieldWidget commandField;
    private final TranscriptWidget transcript;
    private final DevConsoleCommandRunner commandRunner;
    private final JPanel providerContainer;
    private final DevConsoleCompletion completion;
    private final DevConsoleCommandHistory history;
    private final SuggestionOverlayWidget suggestionOverlay = new SuggestionOverlayWidget();
    private final DocumentListener liveFilterListener = new LiveFilterListener();

    public DevConsolePanel(DevConsoleModel model) {
        this(model, WorldRegenerator.noGame(new Random()::nextLong));
    }

    public DevConsolePanel(DevConsoleModel model, WorldRegenerator regenerator) {
        this.cardLayout = new CardLayout();
        this.cards = new JPanel(cardLayout);
        this.commandField = new PatternFieldWidget(".*", "Command");
        this.transcript = new TranscriptWidget();
        this.commandRunner = new DevConsoleCommandRunner(model, transcript, this::showProvider, regenerator);
        this.providerContainer = new JPanel(new BorderLayout());
        providerContainer.setBackground(WidgetTheme.background());
        this.completion = new DevConsoleCompletion(model);
        this.history = new DevConsoleCommandHistory();

        setBackground(WidgetTheme.background());
        setLayout(new BorderLayout());
        setFocusable(false);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(WidgetTheme.windowBorder(), 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        setPreferredSize(DEFAULT_SIZE);

        cards.add(buildSearchView(), SEARCH_CARD);
        cards.add(providerContainer, PROVIDER_CARD);
        add(cards, BorderLayout.CENTER);

        commandField.getTextField().getDocument().addDocumentListener(liveFilterListener);

        bindProviderBackKey();
        showSearchView();
    }

    @Override
    public boolean requestFocusInWindow() {
        return commandField.requestFocusInWindow();
    }

    public JTextField getSearchField() {
        return commandField.getTextField();
    }

    public TranscriptWidget getTranscript() {
        return transcript;
    }

    public DevConsoleCommandHistory getHistory() {
        return history;
    }

    public SuggestionOverlayWidget getSuggestionOverlay() {
        return suggestionOverlay;
    }

    /**
     * Parses and executes whatever is currently typed in the command field, then clears it -
     * public so it can be driven directly both by the Enter keybinding and by tests.
     */
    public void runCommand() {
        String line = commandField.getInput();
        if (!line.isBlank()) {
            transcript.appendCommand(line);
            commandRunner.run(line);
            history.record(line);
        }
        commandField.getTextField().setText("");
    }

    /**
     * Returns from an open detail panel to the top-level transcript view. Public so it can be
     * driven directly both by the Escape keybinding and by tests.
     */
    public void showSearchView() {
        cardLayout.show(cards, SEARCH_CARD);
        commandField.requestFocusInWindow();
    }

    public boolean isProviderPanelShowing() {
        return providerContainer.isVisible();
    }

    /**
     * The currently-opened provider's own panel, or {@code null} if none is open.
     */
    public @Nullable Component getOpenedProviderPanel() {
        return providerContainer.getComponentCount() > 0 ? providerContainer.getComponent(0) : null;
    }

    private JPanel buildSearchView() {
        JPanel searchView = new JPanel(new BorderLayout());
        searchView.setBackground(WidgetTheme.background());
        searchView.add(new HeaderWidget(TITLE), BorderLayout.NORTH);
        searchView.add(buildTranscriptScrollPane(), BorderLayout.CENTER);
        searchView.add(buildCommandField(), BorderLayout.SOUTH);
        return searchView;
    }

    private JScrollPane buildTranscriptScrollPane() {
        JScrollPane scrollPane = ListDetailLayoutUtility.buildScrollPane(transcript);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(COMPONENT_GAP_PX, 0, COMPONENT_GAP_PX, 0));
        return scrollPane;
    }

    private PatternFieldWidget buildCommandField() {
        commandField.setAlignmentX(LEFT_ALIGNMENT);
        commandField.setValidityColoringEnabled(false);
        bindSearchFieldKeys();
        return commandField;
    }

    private void bindSearchFieldKeys() {
        JTextField textField = commandField.getTextField();
        textField.setFocusTraversalKeysEnabled(false);

        InputMap inputMap = textField.getInputMap(WHEN_FOCUSED);
        ActionMap actionMap = textField.getActionMap();

        inputMap.put(Keybindings.MENU_CONFIRM, Keybindings.ACTION_MENU_CONFIRM);
        actionMap.put(Keybindings.ACTION_MENU_CONFIRM, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (suggestionOverlay.isShowing()) {
                    acceptHighlightedSuggestion();
                } else {
                    runCommand();
                }
            }
        });

        inputMap.put(Keybindings.NEXT_TAB, Keybindings.ACTION_DEV_CONSOLE_COMPLETE);
        actionMap.put(Keybindings.ACTION_DEV_CONSOLE_COMPLETE, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (suggestionOverlay.isShowing()) {
                    acceptHighlightedSuggestion();
                }
            }
        });

        inputMap.put(Keybindings.MENU_UP, Keybindings.ACTION_DEV_CONSOLE_HISTORY_UP);
        actionMap.put(Keybindings.ACTION_DEV_CONSOLE_HISTORY_UP, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (suggestionOverlay.isShowing()) {
                    suggestionOverlay.moveHighlightUp();
                } else {
                    recallPreviousCommand();
                }
            }
        });

        inputMap.put(Keybindings.MENU_DOWN, Keybindings.ACTION_DEV_CONSOLE_HISTORY_DOWN);
        actionMap.put(Keybindings.ACTION_DEV_CONSOLE_HISTORY_DOWN, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (suggestionOverlay.isShowing()) {
                    suggestionOverlay.moveHighlightDown();
                } else {
                    recallNextCommand();
                }
            }
        });

        inputMap.put(Keybindings.MENU_CANCEL, Keybindings.ACTION_DEV_CONSOLE_DISMISS_OVERLAY);
        actionMap.put(Keybindings.ACTION_DEV_CONSOLE_DISMISS_OVERLAY, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                suggestionOverlay.hide();
            }
        });
    }

    private class LiveFilterListener implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) {
            refreshSuggestions();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            refreshSuggestions();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            refreshSuggestions();
        }
    }

    private void refreshSuggestions() {
        List<String> cands = completion.candidates(commandField.getInput());
        if (cands.isEmpty()) {
            suggestionOverlay.hide();
        } else {
            // Anchored to commandField itself (not its inner JTextField, which sits inset within
            // PatternFieldWidget's own outline border/padding) and with no top offset, so the
            // overlay sits flush above the field's full bounds - including its floating "Command"
            // label - rather than eating into that reserved space and painting over the label.
            // The horizontal inset keeps the overlay's own left/right edges aligned with the
            // field's actual visible outline rather than its raw (very slightly wider) bounds.
            suggestionOverlay.show(cands, commandField, 0, commandField.getVisibleBoxHorizontalInset());
        }
    }

    private void bindProviderBackKey() {
        InputMap inputMap = providerContainer.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = providerContainer.getActionMap();

        inputMap.put(Keybindings.MENU_CANCEL, Keybindings.ACTION_MENU_CANCEL);
        actionMap.put(Keybindings.ACTION_MENU_CANCEL, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSearchView();
            }
        });
    }

    private void showProvider(DevConsoleModel.SearchResult result) {
        providerContainer.removeAll();
        providerContainer.add(result.provider().createPanel(result.entry().id()), BorderLayout.CENTER);
        cardLayout.show(cards, PROVIDER_CARD);
        providerContainer.revalidate();
        providerContainer.repaint();
        providerContainer.getComponent(0).requestFocusInWindow();
    }

    private void acceptHighlightedSuggestion() {
        String candidate = suggestionOverlay.highlighted();
        String filled = completion.apply(commandField.getInput(), candidate);
        suggestionOverlay.hide();
        setFieldText(filled);
    }

    private void recallPreviousCommand() {
        setFieldText(history.navigateUp(commandField.getInput()));
    }

    private void recallNextCommand() {
        String restored = history.navigateDown();
        if (restored != null) {
            setFieldText(restored);
        }
    }

    private void setFieldText(String text) {
        JTextField textField = commandField.getTextField();
        textField.getDocument().removeDocumentListener(liveFilterListener);
        textField.setText(text);
        textField.setCaretPosition(text.length());
        textField.getDocument().addDocumentListener(liveFilterListener);
    }
}
