package com.swiftfaze.veil.mods;

import com.swiftfaze.veil.entities.dialogue.Dialogue;
import com.swiftfaze.veil.entities.dialogue.DialogueChoice;
import com.swiftfaze.veil.entities.dialogue.DialogueNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ModRegistryDialogueTest {

    private static Dialogue dialogue(String id) {
        return new Dialogue(id, "a", Map.of("a", new DialogueNode("Hi", List.of(new DialogueChoice("Bye", null)))));
    }

    @Test
    void aDialogueIsLookedUpByIdAndAnUnknownIdIsNull() {
        Dialogue greeting = dialogue("test:greeting");
        ModRegistry registry = RegistryFixtures.withDialogues(List.of(greeting));

        assertEquals(greeting, registry.getDialogue("test:greeting"));
        assertNull(registry.getDialogue("test:other"));
    }

    @Test
    void allDialoguesAreListedInLoadOrder() {
        Dialogue first = dialogue("test:first");
        Dialogue second = dialogue("test:second");

        ModRegistry registry = RegistryFixtures.withDialogues(List.of(first, second));

        assertEquals(List.of(first, second), registry.getAllDialogues());
    }
}
