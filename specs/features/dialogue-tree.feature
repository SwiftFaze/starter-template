Feature: Branching dialogue trees and the dialogue popup
  An NPC can carry a dialogue: a JSON-authored tree of nodes, each with an NPC
  line and 1 to 9 choices. A choice either leads to another node or, when it has
  no "next", ends the conversation. Trees may branch to any depth and may loop
  back to earlier nodes. Dialogues live in mods/<mod>/dialogues/*.json
  ({"id", "start", "nodes": {<node id>: {"text", "choices": [{"text", "next"?}]}}})
  and an NPC opts in with an optional "dialogue" field in npcs/*.json naming a
  dialogue id.

  The player starts a conversation with the E key: the four orthogonal
  neighbours are checked in the order N, E, S, W and the first tile holding
  something interactable wins; on one tile an NPC wins over an object. An NPC
  with no dialogue is not interactable. A popup opens with the chat log (NPC
  lines as "<NPC name>: <text>", picked choices as "You: <text>") in the top
  section and the current node's numbered choices in the bottom section.
  Up/Down move the highlight, Enter or the number keys 1-9 pick a choice, and
  Esc ends the conversation at any point. The popup joins the shared world
  pause, so the NPC stops patrolling while you talk. Reopening always starts
  from the "start" node with an empty log.

  Covers: the dialogue JSON kind and its load-time validation, the NPC
  "dialogue" reference, E-key opening and its NPC/object priority, the popup's
  log/choice sections and keyboard selection, ending and Esc, the world pause,
  reopening, and the placeholder conversation on core:town_guard.

  Supersedes: nothing. NPC definition loading and patrol stepping stay owned by
  npc-patrol-framework.feature; the E key's range and ordering for objects stay
  owned by interactable-world-objects.feature; the shared world-pause mechanism
  stays owned by turn-tick-engine.feature. This file only covers what changes
  when an NPC has a dialogue.

  Excludes: any NPC's real dialogue content and quest content (intro-quest
  issue); quest conditions, variables or any state that changes what a node
  says (#71); effects from a choice such as giving items; contextual
  "[E] Talk" hints (#133); remembering conversation progress between
  conversations; a dialogue editor in the sandbox or dev console; pixel
  rendering and colors (verified by the Step 4.5 playtest).

  QA: none - the popup's look and feel is only observable in a running game
  window; key replays do not assert on-screen text. Covered by the Step 4.5
  playtest.

  Background:
    Given the "core" mod is loaded

  # ---- Dialogue definitions ----

  Scenario: Loading a dialogue from a mods directory
    Given a mod declaring dialogue "test:greeting" starting at node "hello"
    And node "hello" says "Well met." with choices "Who are you?" leading to "who" and "Goodbye." ending the conversation
    And node "who" says "Nobody special." with choice "Farewell." ending the conversation
    When the mods directory is loaded
    Then a dialogue with ID "test:greeting" is available
    And its start node is "hello"
    And node "hello" says "Well met." and has 2 choices
    And choice 1 of node "hello" is "Who are you?" and leads to node "who"
    And choice 2 of node "hello" is "Goodbye." and ends the conversation

  Scenario: A dialogue may loop back to an earlier node
    Given a mod declaring dialogue "test:loop" starting at node "a"
    And node "a" says "Ask away." with choice "Again?" leading to "b"
    And node "b" says "Sure." with choices "Back." leading to "a" and "Enough." ending the conversation
    When the mods directory is loaded
    Then a dialogue with ID "test:loop" is available

  Scenario: An NPC references a dialogue by id
    Given a mod declaring dialogue "test:greeting" with a valid tree
    And the mod declares an NPC "test:talker" with dialogue "test:greeting"
    When the mods directory is loaded
    Then NPC "test:talker" has dialogue "test:greeting"

  Scenario: An NPC without a dialogue field has no dialogue
    When the mods directory is loaded
    Then an NPC with no "dialogue" field has no dialogue

  Scenario Outline: An invalid dialogue definition fails to load
    Given a mod declaring dialogue "test:broken" whose <defect>
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming dialogue "test:broken", the reason, and the file it came from

    Examples:
      | defect                                                    |
      | start node "nowhere" does not exist                       |
      | start field is missing                                    |
      | choice leads to "missing", which is not a node            |
      | node "dead_end" has no choices                            |
      | node "crowded" has 10 choices                             |
      | node "orphan" cannot be reached from the start node       |
      | node "hello" has empty text                               |
      | a choice has empty text                                   |

  Scenario: An NPC referencing an unknown dialogue fails to load
    Given a mod declaring an NPC "test:talker" with dialogue "test:no_such_dialogue"
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming NPC "test:talker" and the unknown dialogue "test:no_such_dialogue"

  # ---- Opening a conversation ----

  Scenario: Pressing E next to an NPC with a dialogue opens the dialogue popup
    Given an NPC "Mira" with a dialogue at (5, 4)
    And the player stands at (5, 5)
    When the player presses E
    Then the dialogue popup is shown
    And the popup is headed "Mira"
    And the chat log shows "Mira: " followed by the start node's text
    And the choices show the start node's choices numbered from 1
    And the first choice is highlighted

  Scenario Outline: Each orthogonal neighbour NPC is in range
    Given an NPC with a dialogue at <npc>
    And the player stands at (5, 5)
    When the player presses E
    Then the dialogue popup is shown

    Examples:
      | npc    |
      | (5, 4) |
      | (6, 5) |
      | (5, 6) |
      | (4, 5) |

  Scenario: A diagonal NPC is out of range
    Given an NPC with a dialogue at (6, 4)
    And the player stands at (5, 5)
    When the player presses E
    Then the dialogue popup is not shown

  Scenario: An NPC standing on the player's own tile is out of range
    Given an NPC with a dialogue at (5, 5)
    And the player stands at (5, 5)
    When the player presses E
    Then the dialogue popup is not shown

  Scenario: An NPC without a dialogue is not interactable
    Given an NPC with no dialogue at (5, 4)
    And the player stands at (5, 5)
    When the player presses E
    Then the dialogue popup is not shown

  Scenario: An NPC without a dialogue does not block a neighbour that has one
    Given an NPC with no dialogue at (5, 4)
    And an NPC "Mira" with a dialogue at (6, 5)
    And the player stands at (5, 5)
    When the player presses E
    Then the dialogue popup is shown
    And the popup is headed "Mira"

  Scenario: Neighbours are checked in the order N, E, S, W
    Given an NPC "North" with a dialogue at (5, 4)
    And an NPC "East" with a dialogue at (6, 5)
    And the player stands at (5, 5)
    When the player presses E
    Then the popup is headed "North"

  Scenario: An NPC on a nearer-ordered tile wins over an object
    Given an NPC "Mira" with a dialogue at (5, 4)
    And a closed door at (6, 5)
    And the player stands at (5, 5)
    When the player presses E
    Then the dialogue popup is shown
    And the door at (6, 5) is closed

  Scenario: An object on a nearer-ordered tile wins over an NPC
    Given a closed door at (5, 4)
    And an NPC "Mira" with a dialogue at (6, 5)
    And the player stands at (5, 5)
    When the player presses E
    Then the door at (5, 4) is open
    And the dialogue popup is not shown

  Scenario: On a single tile the NPC wins over the object
    Given an open door at (5, 4)
    And an NPC "Mira" with a dialogue standing on the door at (5, 4)
    And the player stands at (5, 5)
    When the player presses E
    Then the dialogue popup is shown
    And the door at (5, 4) is open

  Scenario: Pressing E with another screen open does not start a conversation
    Given an NPC "Mira" with a dialogue at (5, 4)
    And the player stands at (5, 5)
    And the inventory is open
    When the player presses E
    Then the dialogue popup is not shown

  # ---- Choosing ----

  Scenario Outline: Moving the highlight with the arrow keys
    Given a conversation whose start node has 3 choices
    When the player presses <keys>
    Then choice <highlighted> is highlighted

    Examples:
      | keys           | highlighted |
      | Down           | 2           |
      | Down, Down     | 3           |
      | Down, Down, Down | 3         |
      | Up             | 1           |
      | Down, Up       | 1           |

  Scenario: Picking a choice with Enter advances to the next node
    Given a conversation where choice 2 of the start node leads to node "who"
    When the player presses Down
    And the player presses Enter
    Then the chat log ends with "You: " followed by choice 2's text
    And the chat log then ends with "<NPC name>: " followed by node "who"'s text
    And the choices show node "who"'s choices
    And the first choice is highlighted

  Scenario Outline: Picking a choice with a number key
    Given a conversation whose start node has 3 choices, each leading to a different node
    When the player presses <key>
    Then the chat log shows the text of the node that choice <n> leads to

    Examples:
      | key | n |
      | 1   | 1 |
      | 2   | 2 |
      | 3   | 3 |

  Scenario: A number key with no matching choice is ignored
    Given a conversation whose start node has 2 choices
    When the player presses 3
    Then the chat log is unchanged
    And the dialogue popup is shown

  Scenario: The chat log keeps the whole conversation in order
    Given a conversation that is 3 nodes deep
    When the player picks a choice at each of the first two nodes
    Then the chat log lists, in order, each NPC line and each picked choice
    And the choices show only the third node's choices

  Scenario: Branching leads to different nodes
    Given a conversation whose start node has two choices leading to different nodes
    When the player picks choice 1
    Then the choices show the nodes reached by choice 1
    When the player opens a new conversation and picks choice 2
    Then the choices show the nodes reached by choice 2

  Scenario: A choice that loops back shows the earlier node again
    Given a conversation where choice 1 of node "b" leads back to node "a"
    When the player reaches node "b" and picks choice 1
    Then the chat log ends with node "a"'s text
    And the log still holds the earlier lines

  Scenario: E does nothing inside the popup
    Given a conversation is open
    When the player presses E
    Then the dialogue popup is shown
    And the chat log is unchanged

  Scenario: Movement keys do nothing while the popup is open
    Given a conversation is open
    When the player moves up
    Then the player has not moved

  Scenario Outline: Another screen's key does nothing while the popup is open
    Given a conversation is open
    When the player presses <key>
    Then the <screen> is not shown
    And the dialogue popup is shown
    And the world is paused

    Examples:
      | key | screen    |
      | I   | inventory |
      | X   | codex     |

  # ---- Ending ----

  Scenario: A choice with no next ends the conversation
    Given a conversation is open
    When the player picks a choice that ends the conversation
    Then the dialogue popup is not shown
    And the world is not paused

  Scenario: Esc ends the conversation at the first node
    Given a conversation is open at the start node
    When the player presses Esc
    Then the dialogue popup is not shown
    And the world is not paused

  Scenario: Esc ends the conversation part-way through
    Given a conversation is open at its second node
    When the player presses Esc
    Then the dialogue popup is not shown
    And the pause menu is not shown

  Scenario: Reopening starts again from the start node
    Given a conversation that was ended with Esc at its second node
    When the player presses E next to the same NPC
    Then the chat log shows only the start node's line
    And the choices show the start node's choices
    And the first choice is highlighted

  Scenario: Reopening after finishing starts again from the start node
    Given a conversation that was finished by an ending choice
    When the player presses E next to the same NPC
    Then the chat log shows only the start node's line

  # ---- World pause ----

  Scenario: The world is paused while a conversation is open
    Given an NPC with a dialogue and a patrol
    And a conversation with it is open
    Then the world is paused

  Scenario: The NPC does not patrol while you talk
    Given an NPC with a dialogue and a patrol, standing at (5, 4)
    And the player stands at (5, 5)
    And the player presses E
    When the player presses Down and Up 5 times
    Then the NPC is still at (5, 4)
    And the turn counter has not changed

  Scenario: Patrol resumes after the conversation
    Given an NPC with a dialogue and a patrol, standing at (5, 4)
    And a conversation with it was ended with Esc
    When the player moves in an unblocked direction
    Then the turn counter advances by 1
    And the NPC has taken a step along its patrol

  Scenario: Closing the conversation does not unpause a screen still holding the pause
    Given a conversation is open
    And another screen also holds the world pause
    When the player presses Esc
    Then the dialogue popup is not shown
    And the world is paused

  # ---- Placeholder conversation ----

  Scenario: The town guard has a placeholder conversation
    When the mods directory is loaded
    Then NPC "core:town_guard" has dialogue "core:town_guard_chat"
    And dialogue "core:town_guard_chat" is available

  Scenario: The placeholder conversation really branches
    When the mods directory is loaded
    Then dialogue "core:town_guard_chat" has at least 3 levels from its start node
    And its start node has at least 2 choices leading to different nodes
    And some node reachable from the start node has choices leading to different further nodes
    And at least one choice ends the conversation

  Scenario: Talking to the town guard end to end
    Given the player stands next to the town guard
    When the player presses E
    And the player follows the placeholder conversation down its longest branch to an ending choice
    Then the dialogue popup is not shown
    And the world is not paused

  # Non-goals:
  #   - Real NPC dialogue and quest content (intro-quest issue); the guard's
  #     chat is labelled placeholder content.
  #   - Quest conditions, variables, or text that varies with state (#71).
  #   - Choice effects (giving items, changing state).
  #   - Persisting how far a conversation got; every opening starts fresh.
  #   - A contextual "[E] Talk" controls hint (#133).
  #
  # Risks:
  #   - Adds a WorldPause.DIALOGUE id, a GameListener.openDialogue callback and a
  #     PopupPanels/Main slot; all must follow the chest pattern or a close path
  #     could leave the world paused (see the shared-pause scenario).
  #   - GamePanel's interact scan currently looks at objects only; it must also
  #     ask the NPC list per tile, with NPC-before-object on the same tile.
  #   - NPCs are not solid, so an NPC can sit on the player's tile or in an open
  #     doorway; both cases are specified above.
  #   - ModLoader needs a new "dialogues" kind plus a cross-kind check (NPC ->
  #     dialogue id), and a docs/schemas/dialogue.schema.json + npc.schema.json
  #     "dialogue" property + docs/mod-format.md entry.
  #   - The Esc key reaches the popup before the pause-menu binding; playtest.
  #
  # Open questions: none.
