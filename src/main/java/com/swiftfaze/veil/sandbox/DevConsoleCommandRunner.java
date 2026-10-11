package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.exceptions.ModLoadException;
import com.swiftfaze.veil.ui.widget.TranscriptWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Parses one typed dev-console command line and executes it: `search <term>` filters entries
 * and writes the results into the transcript; `edit <namespace:id>` resolves an entry and hands
 * it to the caller to open. `set`/`add`/`subtract <entry> <field> <value>` mutate a live entity's
 * field via {@link DevConsoleFieldMutator} and write a SUCCESS/ERROR transcript line - only
 * entities whose provider returns one from {@link DevConsoleProvider#fieldMutator} support this.
 * `reload` asks every provider to refresh from mods/ and rebuilds the entry list.
 * `snapshot`/`restore <entry> <name>` capture or restore entity state via
 * {@link DevConsoleSnapshotter}. Anything else writes a specific error line instead of running
 * anything - an unrecognized verb ("Unknown command: ..."), a verb missing its required argument
 * ("Usage: ..."), or an `edit` id that resolves to no entry ("No entry found for id: ...").
 */
public class DevConsoleCommandRunner {

    private static final String SEARCH_VERB = "search";
    private static final String EDIT_VERB = "edit";
    private static final String SET_VERB = "set";
    private static final String ADD_VERB = "add";
    private static final String SUBTRACT_VERB = "subtract";
    private static final String SNAPSHOT_VERB = "snapshot";
    private static final String RESTORE_VERB = "restore";
    private static final String RELOAD_VERB = "reload";
    private static final String REGEN_VERB = "regen";
    private static final String WHITESPACE_RUN = "\\s+";
    private static final int MUTATION_MIN_PARTS = 3;
    private static final int SNAPSHOT_MIN_PARTS = 2;
    private static final List<String> RESULT_HEADERS = List.of("#", "ID", "Name", "Category", "Mod");

    private final DevConsoleModel model;
    private final TranscriptWidget transcript;
    private final Consumer<DevConsoleModel.SearchResult> onEditResolved;
    private final WorldRegenerator regenerator;
    private final Map<String, Consumer<String>> verbHandlers;

    public DevConsoleCommandRunner(DevConsoleModel model, TranscriptWidget transcript,
                                    Consumer<DevConsoleModel.SearchResult> onEditResolved) {
        this(model, transcript, onEditResolved, WorldRegenerator.noGame(new Random()::nextLong));
    }

    public DevConsoleCommandRunner(DevConsoleModel model, TranscriptWidget transcript,
                                    Consumer<DevConsoleModel.SearchResult> onEditResolved,
                                    WorldRegenerator regenerator) {
        this.model = model;
        this.transcript = transcript;
        this.onEditResolved = onEditResolved;
        this.regenerator = regenerator;
        this.verbHandlers = Map.of(
                SEARCH_VERB, this::runSearch,
                EDIT_VERB, this::runEdit,
                SET_VERB, argument -> runMutation(DevConsoleMutationVerb.SET, argument),
                ADD_VERB, argument -> runMutation(DevConsoleMutationVerb.ADD, argument),
                SUBTRACT_VERB, argument -> runMutation(DevConsoleMutationVerb.SUBTRACT, argument),
                SNAPSHOT_VERB, this::runSnapshot,
                RESTORE_VERB, this::runRestore,
                RELOAD_VERB, this::runReload,
                REGEN_VERB, this::runRegen);
    }

    public void run(String commandLine) {
        String trimmed = commandLine.trim();
        String verb = verbOf(trimmed);
        String argument = argumentOf(trimmed);

        Consumer<String> handler = verbHandlers.get(verb);
        if (handler == null) {
            transcript.appendError("Unknown command: " + trimmed);
            return;
        }
        handler.accept(argument);
    }

    private void runSearch(String term) {
        if (term.isBlank()) {
            transcript.appendError("Usage: search <term>");
            return;
        }
        model.setSearchText(term);
        List<DevConsoleModel.SearchResult> results = model.filteredResults();
        transcript.appendInfo(results.size() + " Results found for " + term);
        if (!results.isEmpty()) {
            transcript.appendResultTable(RESULT_HEADERS, resultRows(results));
        }
    }

    private void runEdit(String id) {
        if (id.isBlank()) {
            transcript.appendError("Usage: edit <namespace:id>");
            return;
        }
        Optional<DevConsoleModel.SearchResult> found = model.findById(id);
        if (found.isPresent()) {
            onEditResolved.accept(found.get());
        } else {
            transcript.appendError("No entry found for id: " + id);
        }
    }

    private void runReload(String argument) {
        if (!argument.isBlank()) {
            transcript.appendError("Usage: reload");
            return;
        }
        try {
            int count = model.reload();
            transcript.appendSuccess("Reloaded mods: " + count + " entries");
        } catch (ModLoadException e) {
            transcript.appendError("Reload failed: " + e.getMessage());
        }
    }

    private void runRegen(String argument) {
        if (argument.isBlank()) {
            regenerateAndReport(regenerator.freshSeed());
            return;
        }
        String[] parts = argument.split(WHITESPACE_RUN, 2);
        if (parts.length > 1) {
            transcript.appendError("Usage: regen [seed]");
            return;
        }
        Optional<Long> seed = parseSeed(parts[0]);
        if (seed.isEmpty()) {
            transcript.appendError("Not a valid seed: " + parts[0]);
            return;
        }
        regenerateAndReport(seed.get());
    }

    private static Optional<Long> parseSeed(String token) {
        try {
            return Optional.of(Long.parseLong(token));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private void regenerateAndReport(long seed) {
        Optional<String> problem = regenerator.regenerate(seed);
        if (problem.isPresent()) {
            transcript.appendError(problem.get());
        } else {
            transcript.appendSuccess("World regenerated from seed " + seed);
        }
    }

    private void runMutation(DevConsoleMutationVerb verb, String argument) {
        String[] parts = argument.split(WHITESPACE_RUN, MUTATION_MIN_PARTS);
        if (parts.length < MUTATION_MIN_PARTS) {
            transcript.appendError("Usage: " + verb.name().toLowerCase(Locale.ROOT) + " <entry> <field> <value>");
            return;
        }
        Optional<DevConsoleModel.SearchResult> found = model.findByEntryToken(parts[0]);
        if (found.isEmpty()) {
            transcript.appendError("No entry found for id: " + parts[0]);
            return;
        }
        Optional<DevConsoleFieldMutator> mutator = found.get().provider().fieldMutator(found.get().entry().id());
        if (mutator.isEmpty()) {
            transcript.appendError("Entry does not support field mutation: " + parts[0]);
            return;
        }
        applyMutation(mutator.get(), verb, parts[1], parts[2]);
    }

    private void applyMutation(DevConsoleFieldMutator mutator, DevConsoleMutationVerb verb, String field, String value) {
        DevConsoleMutationResult result = mutator.apply(verb, field, value);
        if (result instanceof DevConsoleMutationResult.Success success) {
            transcript.appendSuccess(success.fieldName() + " set to " + success.newValue());
        } else if (result instanceof DevConsoleMutationResult.Failure failure) {
            transcript.appendError("Invalid input: " + failure.token());
        }
    }

    private void runSnapshot(String argument) {
        withSnapshotter(SNAPSHOT_VERB, argument, (snapshotter, name) ->
                transcript.appendSuccess(snapshotter.takeSnapshot(name)));
    }

    private void runRestore(String argument) {
        withSnapshotter(RESTORE_VERB, argument, (snapshotter, name) -> {
            Optional<String> result = snapshotter.restoreSnapshot(name);
            if (result.isPresent()) {
                transcript.appendSuccess(result.get());
            } else {
                transcript.appendError("No snapshot named: " + name);
            }
        });
    }

    private void withSnapshotter(String verb, String argument, BiConsumer<DevConsoleSnapshotter, String> action) {
        String[] parts = argument.split(WHITESPACE_RUN, SNAPSHOT_MIN_PARTS);
        if (parts.length < SNAPSHOT_MIN_PARTS) {
            transcript.appendError("Usage: " + verb + " <entry> <name>");
            return;
        }
        Optional<DevConsoleModel.SearchResult> found = model.findByEntryToken(parts[0]);
        if (found.isEmpty()) {
            transcript.appendError("No entry found for id: " + parts[0]);
            return;
        }
        Optional<DevConsoleSnapshotter> snapshotter = found.get().provider().snapshotter(found.get().entry().id());
        if (snapshotter.isEmpty()) {
            transcript.appendError("Entry does not support snapshots: " + parts[0]);
            return;
        }
        action.accept(snapshotter.get(), parts[1]);
    }

    private static List<List<String>> resultRows(List<DevConsoleModel.SearchResult> results) {
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            rows.add(rowFor(results.get(i), i + 1));
        }
        return rows;
    }

    private static List<String> rowFor(DevConsoleModel.SearchResult result, int rowNumber) {
        return List.of(String.valueOf(rowNumber), result.entry().id(), result.entry().name(),
                result.entry().category(), result.entry().namespace());
    }

    private static String verbOf(String trimmed) {
        int spaceIndex = trimmed.indexOf(' ');
        return spaceIndex < 0 ? trimmed : trimmed.substring(0, spaceIndex);
    }

    private static String argumentOf(String trimmed) {
        int spaceIndex = trimmed.indexOf(' ');
        return spaceIndex < 0 ? "" : trimmed.substring(spaceIndex + 1).trim();
    }
}
