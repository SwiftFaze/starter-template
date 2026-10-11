Feature: Damage calculation

  Scenario Outline: Calculate damage
    Given a damage value of <x>
    When combined with <y>
    Then the total is checked

    Examples:
      | x | y |
      | 2 | 3 |
      | 4 | 5 |
