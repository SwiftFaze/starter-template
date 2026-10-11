# Verifying a Swing UI change visually

Swing layout/text bugs routinely look fixed by reasoning about the code and
still be wrong on screen — `getPreferredSize()`, `BoxLayout` sizing, and HTML
text wrapping all have non-obvious runtime behavior that reading the source
doesn't reveal. Compiling and passing `mvn test` only proves the code runs,
not that it renders correctly: none of this project's tests assert on pixel
layout or actual rendered text.

This happened concretely while building the compact confirmation popup
(`CompactPopupWidget`, `ResetConfirmationPopup`, `DropConfirmationPopup`):
two separate "fixes" for wrapping long body text were shipped, tests green
both times, and both were still visibly broken — one because this JDK's
Swing HTML renderer silently ignores a CSS pixel `width` when computing
preferred size (confirmed by direct measurement: the "wrapped" size came
back *wider* than the unwrapped text), the other because
`JComponent.getPreferredSize()` returns whatever was last passed to
`setPreferredSize()` verbatim, without recomputing anything, once that's
been called — so a second call with longer text silently kept the first
call's shorter size and clipped a line. Both were only caught by rendering
the actual component and looking at it.

## When to do this

Any time an implementation or fix step (Step 4 in `.claude/workflow.md`, or
a UI-focused change outside that pipeline) touches Swing rendering, layout,
sizing, or text content — **except** glyph-grid content, which approval tests
now cover — not just when a human explicitly asks for a screenshot. Do it
before reporting the change as done, the same way you'd run the test suite
before reporting a logic change as done. This is agent work, done during/after
implementation — it does not replace or overlap with `CLAUDE.md`'s Step 4.5
manual playtest, which is a human verifying real interactive *feel* (movement,
menu navigation, timing) that no static render can capture.

**Approval tests** (see `docs/testing-acceptance.md`) now automatically verify glyph-grid
regressions: camera viewport edges, building footprints, entity-over-tile
layering, and viewport dimensions. That frees this manual process to focus on
genuine Swing concerns — baseline positioning, HTML wrapping, `getPreferredSize()`
staleness — where automated pixel-perfect checking is impractical and human
inspection is the only reliable test.

## How to do it

There is no screenshot tool for a live desktop window. Instead, render the
real component to an image and inspect that with the `Read` tool, the same
way you'd inspect a user-supplied screenshot.

1. Compile the project first (`mvn compile`), so the class you want to
   render exists in `target/classes`.
2. Write a small throwaway Java class in the scratchpad directory (never in
   `src/` — this is a diagnostic, not project code) that:
   - Builds the real component on the Swing event thread
     (`SwingUtilities.invokeAndWait`), calling it exactly the way
     production code does (e.g. `popup.open("Iron Sword")`, not a
     hand-built stand-in).
   - Adds it to an actual `JFrame` and calls `frame.setVisible(true)` —
     the frame must really be shown, not left unrealized.
   - Waits briefly after showing it (`Thread.sleep(500)`–`800`ms) for
     layout/painting to settle, then calls `frame.doLayout()` on the
     frame and its layered pane before capturing.
   - **Captures by painting the component tree directly onto an offscreen
     `BufferedImage`** (`Graphics2D` from
     `image.createGraphics()`, then `component.paint(g2d)`), not with
     `java.awt.Robot`. This is the *opposite* of this doc's own earlier
     guidance — confirmed in this session (2026-09-03) that
     `Robot.createScreenCapture` is unreliable in this dev environment:
     it repeatedly returned a blank/white capture (just the OS window
     chrome, no component content) for a popup that direct `.paint()`
     rendered correctly on the very same run, immediately before and
     after, with nothing else changed. This isn't a one-off — it
     reproduced across multiple popup classes and multiple consecutive
     attempts, including for a component that had captured correctly via
     Robot earlier in that same session. Robot depends on real OS-level
     screen compositing, which this environment doesn't reliably
     provide; direct `.paint()` doesn't. Direct `.paint()` has one known
     narrow caveat from an earlier incident (predating the Robot
     unreliability finding above): it can silently drop per-line styling
     on wrapped rich-text — if a render looks suspicious specifically
     around styled/wrapped text and you have reason to believe you're in
     an environment where Robot actually works, cross-check with Robot
     as a secondary opinion, but don't default to it and don't take a
     blank Robot capture as proof of a rendering bug without first
     confirming the same blank shows up via direct `.paint()`.
   - Disposes the frame immediately after capturing.
3. Compile and run it against the project's classes:
   ```
   javac -cp <project>/target/classes -d . YourDiag.java
   java -cp ".;<project>/target/classes" YourDiag out.png [args...]
   ```
4. Read the PNG with the `Read` tool and actually look at it — don't just
   check that the diagnostic ran without throwing.
5. If something looks wrong and the cause isn't obvious, add a recursive
   component-tree dump (walk `Container.getComponents()`, print each
   child's class, `getBounds()`, `getPreferredSize()`, `getMaximumSize()`,
   and — for a text component — its actual text) before guessing further.
   This is what actually found the stale-`getPreferredSize()` bug below;
   reasoning about the layout math alone had already been wrong twice.
6. Delete or leave the throwaway diagnostic in the scratchpad (it's
   session-local and never committed) once the fix is confirmed.

Re-run this after every attempted fix, not just once per bug — a plausible
explanation for what's wrong is not the same as having verified the new
code actually renders correctly. If a render looks unexpectedly blank,
that's a signal to double-check the diagnostic's own capture method
(see the Robot-vs-`.paint()` note above) before concluding it's a real
application bug.

### Historical note: why this doc used to say the opposite about Robot

The stale-`getPreferredSize()` incident described above (two silently-
broken text-wrapping "fixes," both tests-green) is what originally made
this doc mandate `Robot.createScreenCapture` and warn against direct
`.paint()`/`.printAll()` on an unrealized frame. That underlying lesson —
render the real, actually-shown component and look at it, don't just
reason about layout math — still holds. The specific method recommended
for capturing it doesn't: see the `.paint()`-vs-`Robot` note in "How to
do it" above for why this environment specifically needs the opposite of
what used to be recommended here.

## Relaying "done" up the chain

Producing the PNG and looking at it satisfies the *implementing* agent's
own obligation for this step (see "When to do this" above). It does not
by itself let anyone downstream treat "visual verification passed" as a
fact without checking — an orchestrating session handing the result to
another agent or to a human, or a human being told the step is complete.
A subagent's self-report ("I rendered it and it looked correct") is a
claim, not evidence. Before relaying this step as done, open the actual
PNG yourself (regenerate it if it was already cleaned up) and look — the
same "trust but verify" rule that applies to any other subagent output,
not a special case for this step.
