package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.game.event.GameEvent;
import com.swiftfaze.veil.game.event.GameEventLog;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The one place a main card (title, settings, keybinds, game) is switched. Tracks
 * which card is showing so each real switch records exactly one
 * {@link GameEvent.ScreenChanged}, and moves focus/hints to the new card the way
 * Main's navigation always has.
 */
public class ScreenNavigator {
    private final Consumer<String> showCard;
    private final Function<String, JComponent> cardNamed;
    private final GameEventLog eventLog;
    private @Nullable String currentCard;

    /**
     * The navigator keeps only what it does with the deck (show a card, look one up), not
     * the Swing containers themselves. {@code cards} is read live, so cards registered
     * after construction are still found.
     */
    public ScreenNavigator(CardLayout cardLayout, JPanel cardPanel, Map<String, JComponent> cards,
                           GameEventLog eventLog) {
        this.showCard = name -> cardLayout.show(cardPanel, name);
        this.cardNamed = cards::get;
        this.eventLog = eventLog;
    }

    /**
     * Shows the first card at startup. Records nothing (no screen was left) and leaves
     * focus to the caller, as Main's startup has always handled it itself.
     */
    public void showInitial(String cardName) {
        currentCard = cardName;
        showCard.accept(cardName);
    }

    /** Shows {@code cardName}, recording a ScreenChanged if it differs from the current card. */
    public void navigateTo(String cardName) {
        String previous = currentCard;
        currentCard = cardName;
        show(cardName);
        if (previous != null && !previous.equals(cardName)) {
            eventLog.recordEvent(GameEvent.screenChanged(previous, cardName));
        }
    }

    public @Nullable String getCurrentCard() {
        return currentCard;
    }

    private void show(String cardName) {
        showCard.accept(cardName);
        JComponent target = cardNamed.apply(cardName);
        if (target != null) {
            target.requestFocusInWindow();
            if (target instanceof HintAware hintAware) {
                hintAware.refreshHints();
            }
        }
    }
}
