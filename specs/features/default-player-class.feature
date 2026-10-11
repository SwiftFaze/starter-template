Feature: Default player class
  New players start with a class already assigned so their combat stats
  are usable immediately, without a separate character-creation step.

  Scenario: A newly created player starts as a Warrior
    Given a new player is created
    Then the player's class should be "Warrior"
    And the player's max HP should be 120
