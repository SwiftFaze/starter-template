package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.entities.dialogue.Dialogue;
import com.swiftfaze.veil.entities.dialogue.DialogueChoice;
import com.swiftfaze.veil.entities.dialogue.DialogueNode;
import com.swiftfaze.veil.entities.npc.NonPlayerCharacter;
import com.swiftfaze.veil.entities.npc.NpcDefinition;
import com.swiftfaze.veil.entities.npc.Patrol;
import com.swiftfaze.veil.entities.npc.PatrolMode;
import com.swiftfaze.veil.entities.npc.TileCoord;
import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.game.GameListener;
import com.swiftfaze.veil.game.WorldPause;
import com.swiftfaze.veil.game.event.GameEventLog;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.ui.DialoguePanel;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;

import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;


/**
 * Steps for dialogue-tree.feature, past the loader: an NPC's dialogue reached through the E key, the
 * popup's chat log and choices, ending and Esc, the world pause, and the town guard. Dialogues here
 * are built in code (a loaded one only for the town guard); the keys go through the same bindings
 * the game uses, via {@link GamePanelFixture#pressKey}. Key presses shared with interactable-world-objects
 * ("the player presses ...") are defined in {@link InteractableWorldObjectsSteps}.
 */
public class DialogueSteps {

    private static final String DEFAULT_NAME = "Mira";
    private static final int TALK_X = 5;
    private static final int TALK_Y = 4;
    private static final int PLAYER_X = 5;
    private static final int PLAYER_Y = 5;

    /** Hello, a second node "who" by choice 1, and an ending "Goodbye." as choice 2. */
    private static final Dialogue GREETING = dialogue("hello", nodes(
            node("hello", "Well met.", leadingTo("Who are you?", "who"), end("Goodbye.")),
            node("who", "Nobody special.", end("Farewell."))));
    private static final Dialogue THREE_CHOICES = dialogue("hello", nodes(
            node("hello", "Pick one.", end("One."), end("Two."), end("Three."))));
    private static final Dialogue TWO_CHOICES = dialogue("hello", nodes(
            node("hello", "Pick one.", end("One."), end("Two."))));
    private static final Dialogue WHO_ON_CHOICE_TWO = dialogue("hello", nodes(
            node("hello", "Well met.", end("Goodbye."), leadingTo("Who are you?", "who")),
            node("who", "Nobody special.", end("Farewell."))));
    private static final Dialogue EACH_CHOICE_DIFFERENT = dialogue("hello", nodes(
            node("hello", "Pick a number.", leadingTo("One.", "n1"), leadingTo("Two.", "n2"), leadingTo("Three.", "n3")),
            node("n1", "Node one.", end("Bye one.")),
            node("n2", "Node two.", end("Bye two.")),
            node("n3", "Node three.", end("Bye three."))));
    private static final Dialogue LEFT_OR_RIGHT = dialogue("hello", nodes(
            node("hello", "Pick a side.", leadingTo("Left.", "left"), leadingTo("Right.", "right")),
            node("left", "Left node.", end("Left choice.")),
            node("right", "Right node.", end("Right choice."))));
    private static final Dialogue LOOP_BACK = dialogue("a", nodes(
            node("a", "Ask away.", leadingTo("Again?", "b")),
            node("b", "Sure.", leadingTo("Back.", "a"), end("Enough."))));
    private static final Dialogue THREE_DEEP = dialogue("start", nodes(
            node("start", "Hello there.", leadingTo("Tell me more.", "a"), end("Goodbye.")),
            node("a", "Ask on.", leadingTo("Go deeper.", "b"), end("Enough.")),
            node("b", "Deepest.", end("Farewell."), end("Thanks."))));

    /** The dialogue of the NPC the scenario talks to; the expected text is read from it. */
    private Dialogue talkedTo = GREETING;
    private String npcName = DEFAULT_NAME;
    private final List<Integer> picks = new ArrayList<>();
    private List<String> logWhenOpened = List.of();
    private int playerXWhenOpened;
    private int playerYWhenOpened;
    private int pauseMenuToggles;
    private boolean togglesRecorded;

    // ---- NPCs ----

    @Given("^an NPC (?:\"([^\"]+)\" )?with a dialogue at \\((\\d+), (\\d+)\\)$")
    public void anNpcWithADialogueAt(@Nullable String name, int x, int y) {
        spawnTalker(name == null ? DEFAULT_NAME : name, GREETING, x, y);
    }

    @Given("^an NPC \"([^\"]+)\" with a dialogue standing on the door at \\((\\d+), (\\d+)\\)$")
    public void anNpcWithADialogueStandingOnTheDoor(String name, int x, int y) {
        spawnTalker(name, GREETING, x, y);
    }

    @Given("^an NPC (?:\"([^\"]+)\" )?with no dialogue at \\((\\d+), (\\d+)\\)$")
    public void anNpcWithNoDialogueAt(@Nullable String name, int x, int y) {
        String shown = name == null ? "Passerby" : name;
        game().panel().spawnNpc(new NpcDefinition("test:" + shown.toLowerCase(Locale.ROOT), shown, 'P',
                Color.WHITE, new TileCoord(x, y), null, null));
    }

    @Given("^an NPC with a dialogue and a patrol(?:, standing at \\((\\d+), (\\d+)\\))?$")
    public void anNpcWithADialogueAndAPatrol(@Nullable String x, @Nullable String y) {
        int spawnX = x == null ? TALK_X : Integer.parseInt(x);
        int spawnY = y == null ? TALK_Y : Integer.parseInt(y);
        TileCoord spawn = new TileCoord(spawnX, spawnY);
        Patrol patrol = new Patrol(PatrolMode.PING_PONG, List.of(spawn, new TileCoord(spawnX + 2, spawnY)));
        talkedTo = GREETING;
        npcName = DEFAULT_NAME;
        game().panel().spawnNpc(new NpcDefinition("test:talker", DEFAULT_NAME, 'M', Color.WHITE, spawn, patrol, GREETING));
    }

    @Given("the player stands next to the town guard")
    public void thePlayerStandsNextToTheTownGuard() {
        NpcDefinition guard = ModLoader.load(Paths.get("mods")).getNpc("core:town_guard");
        Assertions.assertNotNull(guard, "core should define core:town_guard");
        Dialogue dialogue = guard.dialogue();
        Assertions.assertNotNull(dialogue, "core:town_guard should have a dialogue");
        talkedTo = dialogue;
        npcName = guard.name();
        game().panel().spawnNpc(new NpcDefinition(guard.id(), guard.name(), guard.symbol(), guard.color(),
                new TileCoord(TALK_X, TALK_Y), null, dialogue));
        game().movePlayerTo(PLAYER_X, PLAYER_Y);
    }

    // ---- Conversations already under way ----

    @Given("a conversation is open")
    public void aConversationIsOpen() {
        openTalkingTo(GREETING);
    }

    @Given("a conversation is open at the start node")
    public void aConversationIsOpenAtTheStartNode() {
        openTalkingTo(GREETING);
    }

    @Given("a conversation is open at its second node")
    public void aConversationIsOpenAtItsSecondNode() {
        openTalkingTo(GREETING);
        pick(1);
    }

    @Given("a conversation with it is open")
    public void aConversationWithItIsOpen() {
        pressE();
    }

    @Given("a conversation with it was ended with Esc")
    public void aConversationWithItWasEndedWithEsc() {
        pressE();
        pressEsc();
    }

    @Given("a conversation that was ended with Esc at its second node")
    public void aConversationThatWasEndedWithEscAtItsSecondNode() {
        openTalkingTo(GREETING);
        pick(1);
        pressEsc();
    }

    @Given("a conversation that was finished by an ending choice")
    public void aConversationThatWasFinishedByAnEndingChoice() {
        openTalkingTo(GREETING);
        pick(2);
    }

    @Given("a conversation whose start node has 3 choices")
    public void aConversationWhoseStartNodeHasThreeChoices() {
        openTalkingTo(THREE_CHOICES);
    }

    @Given("a conversation whose start node has 2 choices")
    public void aConversationWhoseStartNodeHasTwoChoices() {
        openTalkingTo(TWO_CHOICES);
    }

    @Given("a conversation whose start node has 3 choices, each leading to a different node")
    public void aConversationWhoseStartNodeHasThreeChoicesEachLeadingToADifferentNode() {
        openTalkingTo(EACH_CHOICE_DIFFERENT);
    }

    @Given("a conversation whose start node has two choices leading to different nodes")
    public void aConversationWhoseStartNodeHasTwoChoicesLeadingToDifferentNodes() {
        openTalkingTo(LEFT_OR_RIGHT);
    }

    @Given("^a conversation where choice 2 of the start node leads to node \"who\"$")
    public void aConversationWhereChoiceTwoLeadsToWho() {
        openTalkingTo(WHO_ON_CHOICE_TWO);
    }

    @Given("^a conversation where choice 1 of node \"b\" leads back to node \"a\"$")
    public void aConversationWhereChoiceOneOfBLeadsBackToA() {
        openTalkingTo(LOOP_BACK);
    }

    @Given("a conversation that is 3 nodes deep")
    public void aConversationThatIsThreeNodesDeep() {
        openTalkingTo(THREE_DEEP);
    }

    @Given("another screen also holds the world pause")
    public void anotherScreenAlsoHoldsTheWorldPause() {
        game().holdWorldPause(WorldPause.CODEX);
    }

    @Given("the inventory is open")
    public void theInventoryIsOpen() {
        game().holdWorldPause(WorldPause.INVENTORY);
    }

    // ---- Keys and moves ----

    @When("the player presses E next to the same NPC")
    public void thePlayerPressesENextToTheSameNpc() {
        pressE();
    }

    @When("the player picks choice {int}")
    public void thePlayerPicksChoice(int number) {
        pick(number);
    }

    @When("the player picks a choice at each of the first two nodes")
    public void thePlayerPicksAChoiceAtEachOfTheFirstTwoNodes() {
        pick(1);
        pick(1);
    }

    @When("the player picks a choice that ends the conversation")
    public void thePlayerPicksAChoiceThatEndsTheConversation() {
        List<DialogueChoice> choices = currentNode().choices();
        int index = IntStream.range(0, choices.size())
                .filter(i -> choices.get(i).endsConversation())
                .findFirst()
                .orElseThrow();
        pick(index + 1);
    }

    @When("the player reaches node {string} and picks choice {int}")
    public void thePlayerReachesNodeAndPicksChoice(String nodeId, int number) {
        for (int next : DialogueGraph.choicesBetween(talkedTo, currentNodeId(), nodeId)) {
            pick(next);
        }
        pick(number);
    }

    @When("the player opens a new conversation and picks choice {int}")
    public void thePlayerOpensANewConversationAndPicksChoice(int number) {
        pressEsc();
        pressE();
        pick(number);
    }

    @When("the player follows the placeholder conversation down its longest branch to an ending choice")
    public void thePlayerFollowsThePlaceholderConversationDownItsLongestBranch() {
        for (int number : DialogueGraph.longestBranchToEnding(talkedTo)) {
            pick(number);
        }
    }

    @When("the player moves in an unblocked direction")
    public void thePlayerMovesInAnUnblockedDirection() {
        game().pressMoveRight();
    }

    // ---- The popup ----

    @Then("the dialogue popup is shown")
    public void theDialoguePopupIsShown() {
        Assertions.assertTrue(dialoguePanel().isVisible(), "expected the dialogue popup to be shown");
    }

    @Then("the dialogue popup is not shown")
    public void theDialoguePopupIsNotShown() {
        Assertions.assertFalse(dialoguePanel().isVisible(), "expected the dialogue popup to be closed");
    }

    @Then("the popup is headed {string}")
    public void thePopupIsHeaded(String name) {
        Assertions.assertEquals(name, dialoguePanel().title());
    }

    @Then("the chat log shows {string} followed by the start node's text")
    public void theChatLogShowsPrefixFollowedByTheStartNodeText(String prefix) {
        Assertions.assertEquals(List.of(prefix + talkedTo.node(talkedTo.start()).text()), dialoguePanel().logLines());
    }

    @Then("the chat log shows only the start node's line")
    public void theChatLogShowsOnlyTheStartNodesLine() {
        Assertions.assertEquals(List.of(npcName + ": " + talkedTo.node(talkedTo.start()).text()), dialoguePanel().logLines());
    }

    @Then("the chat log is unchanged")
    public void theChatLogIsUnchanged() {
        Assertions.assertEquals(logWhenOpened, dialoguePanel().logLines());
    }

    @Then("the chat log lists, in order, each NPC line and each picked choice")
    public void theChatLogListsInOrderEachNpcLineAndEachPickedChoice() {
        Assertions.assertEquals(replayedLog(), dialoguePanel().logLines());
    }

    @Then("the log still holds the earlier lines")
    public void theChatLogStillHoldsTheEarlierLines() {
        Assertions.assertEquals(replayedLog(), dialoguePanel().logLines());
    }

    @Then("the chat log ends with {string} followed by choice {int}'s text")
    public void theChatLogEndsWithPrefixFollowedByChoiceText(String prefix, int number) {
        List<String> log = dialoguePanel().logLines();
        DialogueNode answered = nodeWithLine(log.get(log.size() - 3));
        Assertions.assertEquals(prefix + answered.choices().get(number - 1).text(), log.get(log.size() - 2));
    }

    @Then("the chat log then ends with {string} followed by node {string}'s text")
    public void theChatLogThenEndsWithPrefixFollowedByNodeText(String prefix, String nodeId) {
        Assertions.assertEquals(prefix.replace("<NPC name>", npcName) + talkedTo.node(nodeId).text(), lastLogLine());
    }

    @Then("the chat log ends with node {string}'s text")
    public void theChatLogEndsWithNodeText(String nodeId) {
        Assertions.assertEquals(npcName + ": " + talkedTo.node(nodeId).text(), lastLogLine());
    }

    @Then("the chat log shows the text of the node that choice {int} leads to")
    public void theChatLogShowsTheTextOfTheNodeThatChoiceLeadsTo(int number) {
        String next = talkedTo.node(talkedTo.start()).choices().get(number - 1).next();
        Assertions.assertNotNull(next, "choice " + number + " of the start node should lead to a node");
        Assertions.assertEquals(npcName + ": " + talkedTo.node(next).text(), lastLogLine());
    }

    @Then("the first choice is highlighted")
    public void theFirstChoiceIsHighlighted() {
        Assertions.assertEquals(1, dialoguePanel().highlightedChoice());
    }

    @Then("choice {int} is highlighted")
    public void choiceIsHighlighted(int number) {
        Assertions.assertEquals(number, dialoguePanel().highlightedChoice());
    }

    @Then("the choices show the start node's choices numbered from 1")
    public void theChoicesShowTheStartNodesChoicesNumberedFromOne() {
        theChoicesShowTheStartNodesChoices();
    }

    @Then("the choices show the start node's choices")
    public void theChoicesShowTheStartNodesChoices() {
        Assertions.assertEquals(numbered(talkedTo.node(talkedTo.start())), dialoguePanel().choiceLabels());
    }

    @Then("the choices show node {string}'s choices")
    public void theChoicesShowNodesChoices(String nodeId) {
        Assertions.assertEquals(numbered(talkedTo.node(nodeId)), dialoguePanel().choiceLabels());
    }

    @Then("the choices show the nodes reached by choice {int}")
    public void theChoicesShowTheNodesReachedByChoice(int number) {
        Assertions.assertEquals(numbered(currentNode()), dialoguePanel().choiceLabels());
    }

    @Then("the choices show only the third node's choices")
    public void theChoicesShowOnlyTheThirdNodesChoices() {
        Assertions.assertEquals(numbered(currentNode()), dialoguePanel().choiceLabels());
    }

    @Then("the player has not moved")
    public void thePlayerHasNotMoved() {
        Assertions.assertEquals(playerXWhenOpened, game().playerX());
        Assertions.assertEquals(playerYWhenOpened, game().playerY());
    }

    @Then("the pause menu is not shown")
    public void thePauseMenuIsNotShown() {
        Assertions.assertEquals(0, pauseMenuToggles, "Esc should not reach the pause menu while the popup is open");
    }

    // ---- Patrols and turns ----

    @Then("^the NPC is still at \\((\\d+), (\\d+)\\)$")
    public void theNpcIsStillAt(int x, int y) {
        NonPlayerCharacter npc = onlyNpc();
        Assertions.assertEquals(new TileCoord(x, y), new TileCoord(npc.getX(), npc.getY()));
    }

    @Then("the NPC has taken a step along its patrol")
    public void theNpcHasTakenAStepAlongItsPatrol() {
        NonPlayerCharacter npc = onlyNpc();
        int distance = Math.abs(npc.getX() - TALK_X) + Math.abs(npc.getY() - TALK_Y);
        Assertions.assertNotEquals(0, distance, "the NPC should have moved off its spawn tile");
        Assertions.assertEquals(1, distance, "the NPC should have moved one tile along its patrol");
    }

    @Then("the turn counter has not changed")
    public void theTurnCounterHasNotChanged() {
        Assertions.assertEquals(0, game().currentTurn(), "a fresh world starts at turn 0");
    }

    @Then("the turn counter advances by 1")
    public void theTurnCounterAdvancesByOne() {
        Assertions.assertEquals(1, game().currentTurn(), "a fresh world starts at turn 0");
    }

    // ---- Helpers ----

    /** Starts a conversation with a fresh NPC on the tile north of the player, opened with E. */
    private void openTalkingTo(Dialogue dialogue) {
        spawnTalker(DEFAULT_NAME, dialogue, TALK_X, TALK_Y);
        game().movePlayerTo(PLAYER_X, PLAYER_Y);
        pressE();
    }

    private void spawnTalker(String name, Dialogue dialogue, int x, int y) {
        talkedTo = dialogue;
        npcName = name;
        game().panel().spawnNpc(new NpcDefinition("test:" + name.toLowerCase(Locale.ROOT), name, 'M',
                Color.WHITE, new TileCoord(x, y), null, dialogue));
    }

    /** Presses E, and if that opened the popup, records what it showed so the later checks can compare against it. */
    private void pressE() {
        ensureToggleCounting();
        boolean wasOpen = dialoguePanel().isVisible();
        game().pressKey(Keybindings.INTERACT);
        if (!wasOpen && dialoguePanel().isVisible()) {
            logWhenOpened = dialoguePanel().logLines();
            playerXWhenOpened = game().playerX();
            playerYWhenOpened = game().playerY();
            picks.clear();
        }
    }

    private static void pressEsc() {
        game().pressKey(Keybindings.MENU_CANCEL);
    }

    private void pick(int number) {
        picks.add(number);
        game().pressKey(KeyStroke.getKeyStroke(KeyEvent.VK_0 + number, 0));
    }

    /** Counts Esc reaching the game's pause toggle, so a regression that lets the popup's Esc leak through is caught. */
    private void ensureToggleCounting() {
        if (togglesRecorded) {
            return;
        }
        togglesRecorded = true;
        game().panel().addGameListener(new GameListener() {
            @Override
            public void updatePlayer(Player player) {
                // Only the toggle events are recorded here.
            }

            @Override
            public void togglePause() {
                pauseMenuToggles++;
            }
        });
    }

    /** The log replayed from the picks made so far, using the scenario's dialogue: what the chat log should say. */
    private List<String> replayedLog() {
        List<String> lines = new ArrayList<>();
        DialogueNode node = talkedTo.node(talkedTo.start());
        lines.add(npcName + ": " + node.text());
        for (int pick : picks) {
            DialogueChoice choice = node.choices().get(pick - 1);
            lines.add("You: " + choice.text());
            if (choice.endsConversation()) {
                break;
            }
            node = talkedTo.node(choice.next());
            lines.add(npcName + ": " + node.text());
        }
        return lines;
    }

    private static String lastLogLine() {
        List<String> log = dialoguePanel().logLines();
        return log.get(log.size() - 1);
    }

    /** The node the NPC last spoke: its line is the latest NPC line in the chat log. */
    private DialogueNode currentNode() {
        return talkedTo.node(currentNodeId());
    }

    private String currentNodeId() {
        List<String> log = dialoguePanel().logLines();
        for (int i = log.size() - 1; i >= 0; i--) {
            if (log.get(i).startsWith(npcName + ": ")) {
                return nodeIdWithText(log.get(i).substring(npcName.length() + 2));
            }
        }
        throw new AssertionError("the chat log has no NPC line: " + log);
    }

    private DialogueNode nodeWithLine(String npcLine) {
        return talkedTo.node(nodeIdWithText(npcLine.substring(npcName.length() + 2)));
    }

    private String nodeIdWithText(String text) {
        for (Map.Entry<String, DialogueNode> entry : talkedTo.nodes().entrySet()) {
            if (entry.getValue().text().equals(text)) {
                return entry.getKey();
            }
        }
        throw new AssertionError("no node says: " + text);
    }

    private static NonPlayerCharacter onlyNpc() {
        List<NonPlayerCharacter> npcs = game().panel().getNpcs();
        Assertions.assertEquals(1, npcs.size(), "expected exactly one NPC in the world");
        return npcs.get(0);
    }

    private static DialoguePanel dialoguePanel() {
        return game().dialoguePanel();
    }

    /** The scenario's game. Also published as the turn game, which the world-pause and turn-counter steps read. */
    private static GamePanelFixture game() {
        GamePanelFixture current = SharedScenarioContext.getGameFixture();
        if (current == null) {
            current = GamePanelFixture.onFlatFloor(GameEventLog.noOp());
            SharedScenarioContext.setGameFixture(current);
        }
        if (SharedScenarioContext.getTurnGame() == null) {
            SharedScenarioContext.setTurnGame(current);
        }
        return current;
    }

    private static List<String> numbered(DialogueNode node) {
        List<DialogueChoice> choices = node.choices();
        return IntStream.range(0, choices.size())
                .mapToObj(i -> (i + 1) + ". " + choices.get(i).text())
                .toList();
    }

    private static Dialogue dialogue(String start, Map<String, DialogueNode> nodes) {
        return new Dialogue("test:talk", start, nodes);
    }

    private static Map<String, DialogueNode> nodes(Map.Entry<String, DialogueNode>... entries) {
        return Map.ofEntries(entries);
    }

    private static Map.Entry<String, DialogueNode> node(String id, String text, DialogueChoice... choices) {
        return Map.entry(id, new DialogueNode(text, List.of(choices)));
    }

    private static DialogueChoice leadingTo(String text, String next) {
        return new DialogueChoice(text, next);
    }

    private static DialogueChoice end(String text) {
        return new DialogueChoice(text, null);
    }
}
