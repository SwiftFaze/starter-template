package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.quests.Quest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestSandboxProviderTest {

    private static final int KILL_COUNT = 3;
    private static final String RATS_ID = "core:rats";
    private static final String RATS_NAME = "Rats";

    private static Quest quest(String id, String name) {
        return new Quest(id, name, new Quest.Objective("kill", "core:goblin", KILL_COUNT), List.of());
    }

    @Test
    void entryNamespaceIsTheIdPrefixBeforeTheColon() {
        QuestSandboxProvider provider = new QuestSandboxProvider(List.of(quest(RATS_ID, RATS_NAME)));

        DevConsoleEntry entry = provider.entries().get(0);

        assertEquals("core", entry.namespace());
        assertEquals("Quests", entry.category());
    }

    @Test
    void entryNamespaceIsEmptyWhenTheIdStartsWithAColon() {
        QuestSandboxProvider provider = new QuestSandboxProvider(List.of(quest(":rats", RATS_NAME)));

        assertEquals("", provider.entries().get(0).namespace());
    }

    @Test
    void entryNamespaceIsTheWholeIdWhenThereIsNoColon() {
        QuestSandboxProvider provider = new QuestSandboxProvider(List.of(quest("rats", RATS_NAME)));

        assertEquals("rats", provider.entries().get(0).namespace());
    }

    @Test
    void createdPanelIsTitledWithTheQuestName() {
        QuestSandboxProvider provider = new QuestSandboxProvider(List.of(quest(RATS_ID, RATS_NAME)));

        InspectableDetailPanel panel = (InspectableDetailPanel) provider.createPanel(RATS_ID);

        assertEquals(RATS_NAME, panel.title());
    }

    @Test
    void createdPanelHasItsFirstTableFocused() {
        QuestSandboxProvider provider = new QuestSandboxProvider(List.of(quest(RATS_ID, RATS_NAME)));

        InspectableDetailPanel panel = (InspectableDetailPanel) provider.createPanel(RATS_ID);

        assertTrue(panel.isTableFocused(0));
    }

    @Test
    void createPanelOfAnUnknownIdIsRejected() {
        QuestSandboxProvider provider = new QuestSandboxProvider(List.of(quest(RATS_ID, RATS_NAME)));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> provider.createPanel("core:nope"));

        assertEquals("Unknown quest id: core:nope", thrown.getMessage());
    }
}
