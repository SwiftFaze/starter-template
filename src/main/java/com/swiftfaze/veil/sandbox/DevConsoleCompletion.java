package com.swiftfaze.veil.sandbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Positional, context-sensitive Tab completion for the dev console command field.
 * Position 0 completes verbs; later positions complete against whatever that verb
 * declares for that argument (entry names/namespaces/categories for search; full
 * namespace:id for edit; entry local-ids and field tokens for set/add/subtract).
 */
public class DevConsoleCompletion {

    private static final String SEARCH_VERB = "search";
    private static final String EDIT_VERB = "edit";
    private static final String SET_VERB = "set";
    private static final String ADD_VERB = "add";
    private static final String SUBTRACT_VERB = "subtract";
    private static final String SNAPSHOT_VERB = "snapshot";
    private static final String RESTORE_VERB = "restore";
    private static final String RELOAD_VERB = "reload";
    private static final String REGEN_VERB = "regen";
    private static final String DEFAULT_VALUE = "default";

    private final DevConsoleModel model;
    private final Map<String, Function<String, List<String>>> entryCandidatesByVerb;

    public DevConsoleCompletion(DevConsoleModel model) {
        this.model = model;
        this.entryCandidatesByVerb = Map.of(
                SEARCH_VERB, this::searchCandidates,
                EDIT_VERB, this::editCandidates,
                SET_VERB, this::mutationEntryCandidates,
                ADD_VERB, this::mutationEntryCandidates,
                SUBTRACT_VERB, this::mutationEntryCandidates,
                SNAPSHOT_VERB, this::snapshotterEntryCandidates,
                RESTORE_VERB, this::snapshotterEntryCandidates);
    }

    /**
     * Candidates for the trailing token of commandLine, empty if none (including when
     * commandLine is blank).
     */
    public List<String> candidates(String commandLine) {
        if (commandLine.isBlank()) {
            return List.of();
        }
        String[] tokens = commandLine.strip().split("\\s+");
        int position = currentPosition(commandLine, tokens);
        String prefix = position < tokens.length ? tokens[position] : "";
        return dedupeAndSort(completionCandidates(tokens, position, prefix));
    }

    /**
     * Reconstructs commandLine with its trailing token replaced by the given candidate -
     * for use once the caller has chosen one (e.g. the highlighted suggestion-overlay row).
     */
    public String apply(String commandLine, String candidate) {
        String[] tokens = commandLine.strip().split("\\s+");
        return fillInPlace(tokens, currentPosition(commandLine, tokens), candidate);
    }

    /**
     * A trailing space (or tab) means the developer has finished the previous token and started
     * a new, still-empty one - e.g. "set player " is mid-typing the field-name argument (position
     * 2, empty prefix), not still completing "player" (position 1). Without this, accepting a
     * suggestion and then pressing space to move on re-suggested the just-accepted word instead
     * of advancing to the next argument.
     */
    private static int currentPosition(String commandLine, String[] tokens) {
        boolean startingNewToken = commandLine.endsWith(" ") || commandLine.endsWith("\t");
        return startingNewToken ? tokens.length : tokens.length - 1;
    }

    /**
     * Computes the completion candidates for the given position and prefix.
     */
    private List<String> completionCandidates(String[] tokens, int position, String prefix) {
        if (tokens.length == 0) {
            return List.of();
        }

        return switch (position) {
            case 0 -> verbCandidates(prefix);
            case 1 -> position1Candidates(tokens[0], prefix);
            case 2 -> position2Candidates(tokens[0], tokens[1], prefix);
            case 3 -> position3Candidates(tokens[0], tokens[1], tokens[2], prefix);
            default -> List.of();
        };
    }

    /**
     * Position 1 candidates depend on the verb.
     */
    private List<String> position1Candidates(String verb, String prefix) {
        Function<String, List<String>> entryCandidates = entryCandidatesByVerb.get(verb);
        return entryCandidates == null ? List.of() : entryCandidates.apply(prefix);
    }

    /**
     * Position 2 candidates (field names) only for mutation verbs.
     */
    private List<String> position2Candidates(String verb, String entryToken, String prefix) {
        return switch (verb) {
            case SET_VERB, ADD_VERB, SUBTRACT_VERB -> mutationFieldCandidates(entryToken, prefix);
            default -> List.of();
        };
    }

    /**
     * Position 3 candidates (values) only for set verb.
     */
    private List<String> position3Candidates(String verb, String entryToken,
                                             String fieldToken, String prefix) {
        if (verb.equals(SET_VERB)) {
            return mutationValueCandidates(entryToken, fieldToken, prefix);
        }
        return List.of();
    }

    /**
     * Position 0: verb candidates from the fixed verb set.
     */
    private static List<String> verbCandidates(String prefix) {
        List<String> verbs = List.of(SEARCH_VERB, EDIT_VERB, SET_VERB, ADD_VERB, SUBTRACT_VERB, RELOAD_VERB,
                SNAPSHOT_VERB, RESTORE_VERB, REGEN_VERB);
        return verbs.stream()
                .filter(v -> v.startsWith(prefix.toLowerCase(Locale.ROOT)))
                .toList();
    }

    /**
     * Position 1, verb=search: candidates from entry names, namespaces, and categories.
     */
    private List<String> searchCandidates(String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> candidates = new ArrayList<>();
        for (DevConsoleModel.SearchResult result : model.allResults()) {
            DevConsoleEntry entry = result.entry();
            if (entry.name().toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                candidates.add(entry.name());
            }
            if (entry.namespace().toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                candidates.add(entry.namespace());
            }
            if (entry.category().toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                candidates.add(entry.category());
            }
        }
        return candidates;
    }

    /**
     * Position 1, verb=edit: candidates from entry full namespace:id tokens.
     */
    private List<String> editCandidates(String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        return model.allResults().stream()
                .map(result -> result.entry().id())
                .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(lowerPrefix))
                .toList();
    }

    /**
     * Position 1, verb=set/add/subtract: candidates from local-id form (part after `:`)
     * of entries whose provider has a field mutator.
     */
    private List<String> mutationEntryCandidates(String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        return model.allResults().stream()
                .filter(result -> result.provider().fieldMutator(result.entry().id()).isPresent())
                .map(result -> localId(result.entry().id()))
                .filter(localId -> localId.toLowerCase(Locale.ROOT).startsWith(lowerPrefix))
                .toList();
    }

    /**
     * Position 1, verb=snapshot/restore: candidates from local-id form (part after `:`)
     * of entries whose provider has a snapshotter.
     */
    private List<String> snapshotterEntryCandidates(String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        return model.allResults().stream()
                .filter(result -> result.provider().snapshotter(result.entry().id()).isPresent())
                .map(result -> localId(result.entry().id()))
                .filter(localId -> localId.toLowerCase(Locale.ROOT).startsWith(lowerPrefix))
                .toList();
    }

    /**
     * Position 2, verb=set/add/subtract: resolve the entry token and offer its field tokens.
     */
    private List<String> mutationFieldCandidates(String entryToken, String prefix) {
        Optional<DevConsoleModel.SearchResult> found = model.findByEntryToken(entryToken);
        if (found.isEmpty()) {
            return List.of();
        }
        Optional<DevConsoleFieldMutator> mutator = found.get().provider()
                .fieldMutator(found.get().entry().id());
        if (mutator.isEmpty()) {
            return List.of();
        }
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        return mutator.get().fieldTokens().stream()
                .filter(token -> token.toLowerCase(Locale.ROOT).startsWith(lowerPrefix))
                .toList();
    }

    /**
     * Position 3, verb=set only: offer `default` if the field has a class-default.
     */
    private List<String> mutationValueCandidates(String entryToken, String fieldToken,
                                                 String prefix) {
        if (!DEFAULT_VALUE.startsWith(prefix.toLowerCase(Locale.ROOT))) {
            return List.of();
        }
        Optional<DevConsoleModel.SearchResult> found = model.findByEntryToken(entryToken);
        if (found.isEmpty()) {
            return List.of();
        }
        Optional<DevConsoleFieldMutator> mutator = found.get().provider()
                .fieldMutator(found.get().entry().id());
        if (mutator.isPresent() && mutator.get().hasClassDefault(fieldToken)) {
            return List.of(DEFAULT_VALUE);
        }
        return List.of();
    }

    /**
     * Deduplicate and sort candidates alphabetically (case-insensitive) for determinism.
     */
    private static List<String> dedupeAndSort(List<String> candidates) {
        Set<String> deduped = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        deduped.addAll(candidates);
        return new ArrayList<>(deduped);
    }

    /**
     * Reconstruct the command line with a single candidate filled in place of the prefix token.
     */
    private static String fillInPlace(String[] tokens, int position, String candidate) {
        StringBuilder commandLine = new StringBuilder();
        for (int i = 0; i < position; i++) {
            commandLine.append(tokens[i]).append(" ");
        }
        commandLine.append(candidate);
        return commandLine.toString();
    }

    /**
     * Extract the local-id form (substring after the last `:`, or the whole string if no `:`).
     */
    private static String localId(String id) {
        int colonIndex = id.indexOf(':');
        return colonIndex < 0 ? id : id.substring(colonIndex + 1);
    }

}
