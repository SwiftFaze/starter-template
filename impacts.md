# Impacts: full PMD rule catalogue (#215)

Every violation the ruleset expansion in #215 measures against the existing
codebase, as `bash .claude/tools/check-clean.sh --all` reports it on
2026-09-12 against `.pmd-clean-code.xml`'s new sections (§11 native rules,
§12 vendored jPinpoint rules) plus the four rules the audit resolved as ADD
against a measured conflict (`MutableStaticState`, `DataClass`,
`UseLocaleWithCaseConversions`, `LawOfDemeter`).

**Nothing here is fixed as part of this change** — this is the input to
future planning, per the issue's own scope. The two exceptions, already
fixed because they were required for the restored Error Prone checks to
compile at all, are noted inline. Violations from rules that were already
active before this issue (`VeilMagicNumber`, `VeilAbbreviatedName`,
`GodClass`, `AvoidDuplicateLiterals`, etc.) are pre-existing debt this issue
didn't create and aren't re-litigated here.

Paths below are relative to `src/main/java/com/swiftfaze/veil/` (main) or
`src/test/java/com/swiftfaze/veil/` (test) unless a full path is shown.

## Already resolved by this change (not deferred)

- **`UseLocaleWithCaseConversions`** — **0 violations.** All 22 existing
  `.toLowerCase()`/`.toUpperCase()` call sites (10 in `src/main`, 12 in
  `src/test` — see intent doc C9) were fixed with `Locale.ROOT` as part of
  restoring `StringCaseLocaleUsage` to ERROR (C1), which is the same defect
  this PMD rule flags. Landed clean.
- **The four restored Error Prone checks** (`EnumOrdinal`,
  `StringCaseLocaleUsage`, `MissingSummary`, `ImmutableEnumChecker`) —
  **0 violations.** All 22 call sites fixed per C1/C9; `mvn clean
  test-compile` is green with all four at ERROR.

## Render-path scoping (C5) — measured, not yet relevant

- **`AvoidInstantiatingObjectsInLoops`** — 27 violations repo-wide (below),
  **0 in the render-scoped path** (`GamePanel`, `PatternFieldWidget`), so the
  narrowing currently changes nothing observable — it just stops these 27
  from ever counting against this rule. Correct fix for each: hoist the
  allocation out of the loop into a field or local computed once. None are
  player-visible (they're refresh/build/JSON-parsing code, not per-frame
  render). Size: S each, mechanical.
  - `steps/ModLoaderSteps.java` (9), `ui/widget/RadioGroupWidget.java` (4),
    `ui/widget/ListWidget.java` (3), `ui/widget/TableWidget.java` (3),
    `mods/ModLoader.java` (2), `ui/CodexPanel.java` (2),
    `ui/SettingsKeybindsPanel.java` (1), `ui/SettingsScreenPanel.java` (2),
    `steps/ApprovalTestsGlyphGridSteps.java` (1).

## jPinpoint suppression-hygiene rules (C3) — measured as expected

- **`UsingSuppressWarnings`** — 3 violations, all on the same three
  `@SuppressWarnings("PMD.ExcessiveParameterList")` annotations:
  `ui/widget/PatternFieldWidget.java`, `ui/widget/RadioGroupWidget.java`,
  `ui/widget/TableWidget.java`. Correct fix (per C3, already decided):
  narrow `ExcessiveParameterList` so it doesn't fire on an `@Override` of a
  third-party interface method (`DocumentFilter.replace`,
  `Border.paintBorder`) whose arity isn't ours to choose — after which all
  three suppressions delete and this rule goes quiet. Not player-visible.
  Size: M (touches `.pmd-minimal.xml`'s shared threshold, needs care not to
  weaken the rule elsewhere).
- **`UsingSuppressWarningsHighRisk`** — **0 violations**, exactly as C3
  predicted: its `ruleIdMatches` regex doesn't match
  `"PMD.ExcessiveParameterList"`, so it correctly stays quiet on all three
  sites the plain `UsingSuppressWarnings` above catches. No action needed.

## LawOfDemeter (C2) — measured as expected

- **`LawOfDemeter`** — 73 violations across 21 files, concentrated in Swing
  panels (`steps/DevConsoleSteps.java` 21, `game/GamePanelTest.java` 8,
  `ui/SettingsScreenPanel.java` 6, `ui/widget/PatternFieldWidget.java` 6,
  `entities/player/PlayerInfoTest.java` 4, `Main.java` 3, plus 15 more files
  with 1-3 each). Correct fix varies by site: most are genuine `getX().getY()`
  chains inherent to the Swing API (`component.getParent().getWidth()`-shaped)
  that would need a wrapper/facade method to satisfy the rule without adding
  real value — the checklist judgment from `docs/clean-code-gate.md` applies
  per site. Not player-visible. Size: L overall (21 files) but each site is
  XS-S in isolation; likely a dedicated follow-up issue given the volume.

## MutableStaticState (previously excluded, now ADD)

- **`MutableStaticState`** — 13 violations, all in
  `ui/widget/WidgetTheme.java` (its 13 `public static Color` slots,
  repopulated by `applyTheme()` from a mod-loaded theme). Correct fix: the
  theme mechanism needs rework — e.g. a `ThemeHolder` singleton exposing
  `Color` accessors backed by a mutable instance field, so the mutability is
  encapsulated behind a method instead of a public static field. Not
  player-visible (internal theming mechanism only). Size: L — touches every
  call site that reads a `WidgetTheme.SOME_COLOR` constant across the UI
  layer; a dedicated follow-up issue.

## DataClass (previously excluded, now ADD)

- **`DataClass`** — 10 violations: `GameConst.java`,
  `config/SettingsConfig.java`, `entities/buildings/Building.java`,
  `entities/items/Item.java`, `entities/player/Level.java`,
  `entities/player/PlayerInfo.java`, plus 4 more (records/config holders).
  Correct fix: none, in most cases — these are intentionally pure data
  holders (config, entity definitions) with no behaviour to add; PMD's own
  rule description over-fires on this shape. Recorded as accepted, not
  fixed, per the same reasoning `docs/clean-code-gate.md` already applies to
  other heuristic proxies. Not player-visible. Size: N/A (no code change
  intended — a candidate for a future `.pmd-clean-code.xml` exclusion if it
  keeps false-firing this way, decided with evidence, not preemptively).

## Remaining new-rule violations (native + vendored, 50 rules)

Grouped by rough theme. Count is repo-wide; top files shown, `...` means
more exist. "Fix" is the one-sentence correct remediation; "Player-visible"
and "Size" follow.

### Structural / class design

| Rule | Count | Top files | Fix | Player-visible | Size |
|---|---|---|---|---|---|
| `ConstructorCallsOverridableMethod` | 120 | `ui/SettingsKeybindsPanel.java`(13), `ui/SettingsScreenPanel.java`(10), `ui/TitleScreenPanel.java`(9), `ui/widget/PatternFieldWidget.java`(8), +6 more | Swing panels calling their own overridable `paintComponent`/layout setup methods from the constructor — the idiomatic Swing pattern PMD flags as generically unsafe. Mark the called methods `final` or `private` where subclassing isn't intended. | No | L (120 sites, mostly mechanical `final` additions, needs per-class check for real subclassing) |
| `FieldDeclarationsShouldBeAtStartOfClass` | 99 | `world/CoreTiles.java`(21), `steps/ModLoaderSteps.java`(21), `ui/SettingsKeybindsPanel.java`(14), `GameConst.java`(8), +6 more | Move field declarations above methods/constructors in each class. | No | M (mechanical reordering, but 99 sites) |
| `ClassWithOnlyPrivateConstructorsShouldBeFinal` | 11 | `game/GamePanel.java`(4), `ui/widget/PatternFieldWidget.java`(4), +3 more | Add `final` to classes whose only constructors are private. | No | XS |
| `InstantiableUtilityClass` | 6 | `GameConst.java`, `testing/approval/ApprovalCheck.java`, `ui/GameWindow.java`, `ui/SettingsKeybindsWindow.java`, `ui/SettingsWindow.java`, `steps/SharedScenarioContext.java` | Add a private no-arg constructor to each all-static class. | No | XS |
| `ImplementEqualsHashCodeOnValueObjects` | 6 | `config/SettingsConfig.java`, `entities/player/PlayerInfo.java`, `entities/player/classes/PlayerClass.java`, `entities/quests/Quest.java`, `sandbox/ClassDetailPanel.java`, `ui/widget/ControlsHintBarWidget.java` | Add `equals`/`hashCode` (or convert to a `record` where identity is purely structural). | Possibly — if any of these are used as `Map`/`Set` keys or compared today via reference equality, adding value equality changes behavior; verify per class before fixing. | M |
| `AvoidExposingMutableRecordState` | 19 | `mods/ModRegistry.java`(6), `mods/ModLoader.java`(5), `steps/ModLoaderSteps.java`(4), +3 more | Wrap record accessors returning mutable collections/arrays in `List.copyOf`/`Collections.unmodifiableX`. | No | M |
| `AvoidMutableLists` | 5 | `ui/widget/TableWidget.java`(2), `entities/quests/Quest.java`, `sandbox/ClassSandboxModel.java`, `ui/widget/SuggestionOverlayWidget.java` | Same as above — return an immutable copy or `List.copyOf`. | No | S |
| `ImmutableField` | 4 | `ui/SettingsScreenPanel.java`(2), `steps/UiComponentFrameworkSteps.java`(2) | Add `final` to fields only ever assigned in the constructor. | No | XS |
| `LooseCoupling` | 2 | `ModuleDependencyTest.java`, `NoDuplicateStepDefinitionsTest.java` | Declare the field/variable by interface type (`List`/`Set`) instead of the concrete implementation. | No | XS |
| `MethodReturnsInternalArray` | 1 | `entities/buildings/Building.java` | Return `array.clone()` or switch the field to a `List`. | No | XS |
| `ArrayIsStoredDirectly` | 1 | `entities/buildings/Building.java` | Store `array.clone()` in the constructor instead of the passed-in reference. | No | XS |

### Error handling

| Rule | Count | Top files | Fix | Player-visible | Size |
|---|---|---|---|---|---|
| `AvoidRethrowingException` | 8 | `mods/ModLoader.java`(8) | Remove the redundant catch-and-rethrow, or catch a more specific type and wrap with context. | Mod-author-visible (error messages during mod loading) — verify wording is preserved. | S |
| `CloseResource` | 1 | `ui/TitleScreenPanel.java` | Wrap the resource in try-with-resources. | No | XS |
| `EmptyControlStatement` | 1 | `steps/UiComponentFrameworkSteps.java` | Remove the empty block or add the intentional no-op comment PMD's `allowCommentedBlocks` would still flag — decompose instead. | No | XS |
| `ExceptionAsFlowControl` | 1 | `steps/ApprovalTestsGlyphGridSteps.java` | Replace the exception-driven control flow with a normal conditional/return. | No | XS |
| `ReturnEmptyCollectionRatherThanNull` | 1 | `steps/DevConsoleSteps.java` | Return `List.of()`/`Collections.emptyList()` instead of `null`. | Possibly — callers that null-check today would need the same audit before the change lands. | S |
| `NullAssignment` | 1 | `steps/DevConsoleSteps.java` | Replace the `= null` with an `Optional` or a sentinel that makes the "no value" case explicit. | No | XS |
| `AssignmentInOperand` | 7 | `steps/ModLoaderSteps.java`(6), `mods/CalcExpressionParser.java`(1) | Pull the assignment out of the condition/expression onto its own statement before the check. | No | XS |

### Style / redundancy

| Rule | Count | Top files | Fix | Player-visible | Size |
|---|---|---|---|---|---|
| `ControlStatementBraces` | 33 | `mods/CalcExpressionParser.java`(8), `ui/widget/TableWidget.java`(7), `mods/ModLoader.java`(6), `sandbox/DevConsoleCommandHistory.java`(5), +2 more | Add braces to every brace-less `if`/`for`/`while` body. | No | S (mechanical) |
| `UnnecessaryFullyQualifiedName` | 16 | `ui/SettingsKeybindsPanel.java`(3), `ui/SettingsScreenPanel.java`(3), `ui/TitleScreenPanel.java`(2), +4 more | Replace the fully-qualified reference with an import. | No | XS (mechanical) |
| `RedundantFieldInitializer` | 18 | `ui/SettingsKeybindsPanel.java`(3), `ui/widget/TableWidget.java`(3), `config/SettingsConfig.java`(2), +4 more | Remove the explicit `= 0`/`= null`/`= false` initializer the JVM already applies. | No | XS (mechanical) |
| `LiteralsFirstInComparisons` | 20 | `mods/CalcExpressionParser.java`(10), `steps/ModLoaderSteps.java`(3), +4 more | Flip `variable.equals("literal")` to `"literal".equals(variable)`. | No | XS (mechanical) |
| `UseExplicitTypes` | 18 | `steps/DevConsoleSteps.java`(7), `mods/ModLoader.java`(4), `config/SettingsRepository.java`(2), +4 more | Replace `var` with the explicit type where PMD's rule wants it spelled out. | No | XS (mechanical) |
| `LambdaCanBeMethodReference` | 9 | `game/GamePanel.java`(4), `ui/PauseMenuPopupTest.java`(2), `ui/SettingsScreenPanelTest.java`(2), `steps/PauseScreenSteps.java`(1) | Convert the lambda to a method reference. | No | XS (mechanical) |
| `ConfusingTernary` | 1 | `ui/SettingsKeybindsPanel.java` | Invert the condition so the ternary's positive branch comes first. | No | XS |
| `ModifierOrder` | 1 | `world/WorldScene.java` | Reorder modifiers to the JLS-canonical order. | No | XS |
| `VariableDeclarationUsageDistance` | 1 | `game/GamePanel.java` | Move the declaration closer to its first use. | No | XS |
| `EmptyMethodInAbstractClassShouldBeAbstract` | 2 | `world/WorldScene.java`(2) | Make the two empty methods `abstract` instead of giving them empty bodies. | No | XS |
| `ForLoopCanBeForeach` | 1 | `steps/UiComponentFrameworkSteps.java` | Convert the indexed loop to a foreach. | No | XS |
| `UselessOverridingMethod` | 1 | `ui/widget/CompactPopupWidget.java` | Delete the override that only calls `super`. | No | XS |

### Imports / suppressions / dead surface

| Rule | Count | Top files | Fix | Player-visible | Size |
|---|---|---|---|---|---|
| `ExcessiveImports` | 3 | `Main.java`, `steps/ModLoaderSteps.java`, `steps/UiComponentFrameworkSteps.java` | Split the class, or accept as a composition-root/step-definition exception (same reasoning as `docs/clean-code-gate.md`'s existing carve-outs). | No | S (judgment call per class) |
| `TooManyStaticImports` | 4 | `ModuleDependencyTest.java`, `steps/DevConsoleSteps.java`, `steps/UiComponentFrameworkSteps.java`, `ui/DetailsPaneWidgetTest.java` | Import the containing class and qualify calls instead of static-importing each member. | No | XS |
| `UnnecessaryWarningSuppression` | 3 | `ui/widget/PatternFieldWidget.java`, `ui/widget/RadioGroupWidget.java`, `ui/widget/TableWidget.java` | Once the `ExcessiveParameterList` narrowing above lands, these three `@SuppressWarnings` become genuinely unnecessary and delete. | No | XS (blocked on the jPinpoint fix above) |
| `UnsupportedJdkApiUsage` | 2 | `ui/widget/TableWidgetTest.java`(2) | Replace the flagged JDK API call with the supported alternative PMD's message names. | No | XS |
| `UnnecessaryConstructor` | 2 | `GameConst.java`, `mods/ModSchemaValidator.java` | Delete the constructor the compiler would generate anyway. | No | XS |
| `InsufficientStringBufferDeclaration` | 1 | `mods/ModSchemaValidator.java` | Size the `StringBuilder` constructor to the expected result length. | No | XS |
| `AppendCharacterWithChar` | 3 | `mods/ModSchemaValidator.java`, `sandbox/DevConsoleCompletion.java`, `steps/ApprovalTestsGlyphGridSteps.java` | Use `sb.append('x')` instead of `sb.append("x")` for single characters. | No | XS |
| `AvoidReStreamingEnumValues` | 1 | `sandbox/PlayerField.java` | Cache `values()` in a static field instead of calling it per-lookup, or use an `EnumMap`. | No | XS |
| `AvoidAccessibilityAlteration` | 1 | `ui/widget/SuggestionOverlayWidgetTest.java` | Remove the reflective accessibility change; expose test-only access a supported way (package-private, a test-only constructor). | No | S |
| `AvoidImplicitlyRecompilingRegex` | 4 | `sandbox/DevConsoleCompletion.java`(2), `testing/approval/ApprovalCheck.java`, `testing/approval/ApprovalReapprove.java` | Hoist the regex to a `static final Pattern`. | No | XS |
| `SignatureDeclareThrowsException` | 14 | `steps/UiComponentFrameworkSteps.java`(12), `steps/WorldSingleFloorRenderingSteps.java`(1), `ui/widget/SuggestionOverlayWidgetTest.java`(1) | Narrow `throws Exception` to the specific checked exceptions each method actually throws. | No | S (mostly one file) |
| `TestClassWithoutTestCases` | 2 | `RunCucumberTest.java`, `steps/ApprovalTestsGlyphGridSteps.java` | Both are legitimate non-`@Test` classes (a `@Suite` runner and a step-definition class) — false positive on this codebase's Cucumber shape; accept, don't fix. | No | N/A |
| `TooFewBranchesForSwitch` | 4 | `steps/DevConsoleSteps.java`(2), `steps/UiComponentFrameworkSteps.java`(2) | Convert the 2-branch `switch` to an `if`/`else`. | No | XS |
| `SwitchDensity` | 1 | `steps/ModLoaderSteps.java` | Reduce statements-per-case, or extract case bodies into named methods. | No | S |

### Test quality (routed to `src/test` only)

| Rule | Count | Top files | Fix | Size |
|---|---|---|---|---|
| `JUnitJupiterTestShouldBePackagePrivate` | 34 | `ui/widget/TableWidgetTest.java`(14), `ui/widget/RadioGroupWidgetTest.java`(8), `ui/widget/PatternFieldWidgetTest.java`(7), `ui/widget/SliderWidgetTest.java`(5) | Drop `public` from each `@Test` method's modifier list. | XS (mechanical, 4 files) |

## Total

50 of the 215 newly-added rules (196 native + 19 vendored jPinpoint)
produced at least one violation — 44 native, 6 jPinpoint (including
`LawOfDemeter`, `MutableStaticState`, `DataClass` and
`AvoidInstantiatingObjectsInLoops`, each covered in its own section above
for the reasons the intent doc's clarifications call out). 152 of the 196
native rules and 13 of the 19 vendored jPinpoint rules produced zero
violations — the codebase already satisfies them. `UseLocaleWithCaseConversions`
and the four restored Error Prone checks already landed clean as a required
part of this change, so none of their would-be violations appear above.

## forbidden-apis gate (#218)

0 unfixable call sites; 4 call sites fixed by this change to satisfy the
jdk-unsafe and jdk-deprecated signature bundles (Java 17 target).

### Call sites fixed

All violations were default-locale uses of `String.format()` where
`String.format(Locale.ROOT, ...)` is the appropriate fix (non-user-visible
formatting for logging, testing, and dev console display).

| File | Line | Method | Call | Fix |
|---|---|---|---|---|
| `testing/approval/ApprovalCheck.java` | 39 | `failWithReceived` | `String.format("Approval test failed...")` | Add `Locale.ROOT` as first format argument |
| `ui/widget/TranscriptWidget.java` | 107 | `padded` | `String.format("%-" + width + "s", text)` | Add `Locale.ROOT` as first format argument |
| `sandbox/ClassSandboxPanel.java` | 96 | `refresh` | `String.format("ATK %d ...")` | Add `Locale.ROOT` as first format argument |
| `steps/ClassSandboxSteps.java` | 79 | `assertStatsLabelShows` | `String.format("ATK %d ...")` | Add `Locale.ROOT` as first format argument |

All 4 fixes are mechanical (add `Locale.ROOT` argument); no behavioral change —
all existing inputs (ASCII/UTF-8 content) produce identical output.
