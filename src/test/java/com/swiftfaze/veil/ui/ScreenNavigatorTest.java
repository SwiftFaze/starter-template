package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.game.event.GameEvent;
import com.swiftfaze.veil.game.event.GameEventLog;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenNavigatorTest {
    private static final String TITLE = "title";
    private static final String SETTINGS = "settings";
    private static final String GAME = "game";
    private static final String HINTED = "hinted";

    private final GameEventLog log = GameEventLog.inMemory();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final Map<String, JComponent> cards = new HashMap<>();
    private final ScreenNavigator navigator = new ScreenNavigator(cardLayout, cardPanel, cards, log);

    ScreenNavigatorTest() {
        for (String name : List.of(TITLE, SETTINGS, GAME)) {
            JPanel card = new JPanel();
            cardPanel.add(card, name);
            cards.put(name, card);
        }
    }

    @Test
    void initialShowRecordsNothing() {
        navigator.showInitial(TITLE);
        assertEquals(TITLE, navigator.getCurrentCard());
        assertTrue(log.getEvents().isEmpty());
    }

    @Test
    void eachRealSwitchRecordsOneScreenChanged() {
        navigator.showInitial(TITLE);
        navigator.navigateTo(SETTINGS);
        navigator.navigateTo(TITLE);
        assertEquals(List.of(new GameEvent.ScreenChanged(TITLE, SETTINGS),
                new GameEvent.ScreenChanged(SETTINGS, TITLE)), log.getEvents());
        assertTrue(cards.get(TITLE).isVisible());
    }

    @Test
    void switchingToTheCurrentCardRecordsNothing() {
        navigator.showInitial(GAME);
        navigator.navigateTo(GAME);
        assertTrue(log.getEvents().isEmpty());
        assertEquals(GAME, navigator.getCurrentCard());
    }

    @Test
    void showInitialMakesOnlyThatCardVisible() {
        navigator.showInitial(GAME);
        assertTrue(cards.get(GAME).isVisible());
        assertFalse(cards.get(TITLE).isVisible());
    }

    @Test
    void navigateToMakesOnlyThatCardVisible() {
        navigator.showInitial(TITLE);
        navigator.navigateTo(SETTINGS);
        assertTrue(cards.get(SETTINGS).isVisible());
        assertFalse(cards.get(TITLE).isVisible());
    }

    @Test
    void navigateToRefreshesHintsOnAHintAwareCard() {
        RecordingHintCard hinted = new RecordingHintCard();
        cardPanel.add(hinted, HINTED);
        cards.put(HINTED, hinted);
        navigator.showInitial(TITLE);
        navigator.navigateTo(HINTED);
        assertEquals(1, hinted.refreshCount);
    }

    private static final class RecordingHintCard extends JPanel implements HintAware {
        private int refreshCount;

        @Override
        public void refreshHints() {
            refreshCount++;
        }
    }
}
