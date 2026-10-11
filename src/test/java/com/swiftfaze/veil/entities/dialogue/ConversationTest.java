package com.swiftfaze.veil.entities.dialogue;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationTest {

    private static final Dialogue GREETING = new Dialogue("test:greeting", "hello", Map.of(
            "hello", new DialogueNode("Well met.", List.of(
                    new DialogueChoice("Who are you?", "who"),
                    new DialogueChoice("Goodbye.", null))),
            "who", new DialogueNode("Nobody special.", List.of(
                    new DialogueChoice("Farewell.", null)))));

    @Test
    void startsOnTheStartNodeWithItsLineInTheLog() {
        Conversation conversation = new Conversation("Mira", GREETING);

        assertEquals(List.of("Mira: Well met."), texts(conversation));
        assertEquals("Well met.", conversation.currentNode().text());
        assertFalse(conversation.isEnded());
    }

    @Test
    void choosingALinkedChoiceLogsThePickThenTheNextNodesLine() {
        Conversation conversation = new Conversation("Mira", GREETING);

        assertTrue(conversation.choose(0));

        assertEquals(List.of("Mira: Well met.", "You: Who are you?", "Mira: Nobody special."), texts(conversation));
    }

    @Test
    void choosingALinkedChoiceMovesToTheNextNodeWithoutEnding() {
        Conversation conversation = new Conversation("Mira", GREETING);

        conversation.choose(0);

        assertEquals("Nobody special.", conversation.currentNode().text());
        assertFalse(conversation.isEnded());
    }

    @Test
    void choosingAnEndingChoiceLogsItAndEndsTheConversation() {
        Conversation conversation = new Conversation("Mira", GREETING);

        assertTrue(conversation.choose(1));

        assertEquals(List.of("Mira: Well met.", "You: Goodbye."), texts(conversation));
        assertTrue(conversation.isEnded());
    }

    @Test
    void anOutOfRangeChoiceIsIgnored() {
        Conversation conversation = new Conversation("Mira", GREETING);

        assertFalse(conversation.choose(2));
        assertFalse(conversation.choose(-1));

        assertEquals(List.of("Mira: Well met."), texts(conversation));
    }

    @Test
    void anOutOfRangeChoiceDoesNotEndTheConversation() {
        Conversation conversation = new Conversation("Mira", GREETING);

        conversation.choose(2);
        conversation.choose(-1);

        assertFalse(conversation.isEnded());
    }

    @Test
    void aChoiceAfterTheConversationEndedIsIgnored() {
        Conversation conversation = new Conversation("Mira", GREETING);
        conversation.choose(1);

        assertFalse(conversation.choose(0));

        assertEquals(List.of("Mira: Well met.", "You: Goodbye."), texts(conversation));
    }

    @Test
    void aLoopingChoiceShowsTheEarlierNodeAgainAndKeepsTheEarlierLines() {
        Dialogue loop = new Dialogue("test:loop", "a", Map.of(
                "a", new DialogueNode("Ask away.", List.of(new DialogueChoice("Again?", "b"))),
                "b", new DialogueNode("Sure.", List.of(
                        new DialogueChoice("Back.", "a"),
                        new DialogueChoice("Enough.", null)))));
        Conversation conversation = new Conversation("Mira", loop);

        conversation.choose(0);
        conversation.choose(0);

        assertEquals(List.of("Mira: Ask away.", "You: Again?", "Mira: Sure.", "You: Back.", "Mira: Ask away."),
                texts(conversation));
    }

    @Test
    void aFreshConversationStartsAgainFromTheStartNode() {
        Conversation first = new Conversation("Mira", GREETING);
        first.choose(0);

        Conversation reopened = new Conversation("Mira", GREETING);

        assertEquals(List.of("Mira: Well met."), texts(reopened));
    }

    private static List<String> texts(Conversation conversation) {
        return conversation.log().stream().map(Conversation.Line::text).toList();
    }
}
