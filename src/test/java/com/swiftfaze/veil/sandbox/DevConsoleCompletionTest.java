package com.swiftfaze.veil.sandbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevConsoleCompletionTest {

    private static final String PLAYER_ID = "core:player";
    private static final String PLAYER_NAME = "player";
    private static final String STR_TOKEN = "str";

    @Test
    void mutationEntryCandidatesExcludeEntriesWithoutAFieldMutator() {
        DevConsoleModel model = new DevConsoleModel(List.of(
                mutatorProvider(PLAYER_ID, PLAYER_NAME),
                plainProvider("core:pete", "pete")
        ));
        DevConsoleCompletion completion = new DevConsoleCompletion(model);

        List<String> candidates = completion.candidates("set p");
        String filled = completion.apply("set p", candidates.get(0));

        assertEquals(List.of(PLAYER_NAME), candidates);
        assertEquals("set player", filled);
    }

    @Test
    void mutationEntryCandidatesExcludeNonMatchingPrefixes() {
        DevConsoleModel model = new DevConsoleModel(List.of(mutatorProvider(PLAYER_ID, PLAYER_NAME)));
        DevConsoleCompletion completion = new DevConsoleCompletion(model);

        List<String> candidates = completion.candidates("set z");

        assertTrue(candidates.isEmpty());
    }

    @Test
    void mutationEntryLocalIdFallsBackToTheWholeIdWhenThereIsNoNamespaceColon() {
        DevConsoleModel model = new DevConsoleModel(List.of(mutatorProvider("standalone", "Standalone")));
        DevConsoleCompletion completion = new DevConsoleCompletion(model);

        List<String> candidates = completion.candidates("set stand");

        assertEquals(List.of("standalone"), candidates);
    }

    @ParameterizedTest
    @ValueSource(strings = {"snapshot", "restore"})
    void snapshotEntryCandidatesExcludeEntriesWithoutASnapshotter(String verb) {
        DevConsoleModel model = new DevConsoleModel(List.of(
                snapshotterProvider(PLAYER_ID, PLAYER_NAME),
                plainProvider("core:pete", "pete")
        ));
        DevConsoleCompletion completion = new DevConsoleCompletion(model);
        String commandLine = verb + " p";

        List<String> candidates = completion.candidates(commandLine);
        String filled = completion.apply(commandLine, candidates.get(0));

        assertEquals(List.of(PLAYER_NAME), candidates);
        assertEquals(verb + " player", filled);
    }

    @ParameterizedTest
    @ValueSource(strings = {"snapshot", "restore"})
    void snapshotEntryCandidatesExcludeNonMatchingPrefixes(String verb) {
        DevConsoleModel model = new DevConsoleModel(List.of(snapshotterProvider(PLAYER_ID, PLAYER_NAME)));
        DevConsoleCompletion completion = new DevConsoleCompletion(model);

        assertTrue(completion.candidates(verb + " z").isEmpty());
    }

    @Test
    void snapshotEntryCandidatesMatchThePrefixCaseInsensitively() {
        DevConsoleModel model = new DevConsoleModel(List.of(snapshotterProvider(PLAYER_ID, PLAYER_NAME)));
        DevConsoleCompletion completion = new DevConsoleCompletion(model);

        assertEquals(List.of(PLAYER_NAME), completion.candidates("snapshot PL"));
    }
    @Test
    void trailingSpaceStartsANewEmptyPrefixArgumentRatherThanReSuggestingTheJustTypedWord() {
        DevConsoleModel model = new DevConsoleModel(List.of(
                mutatorProviderWithFields(PLAYER_ID, PLAYER_NAME, List.of(STR_TOKEN, "dex"))
        ));
        DevConsoleCompletion completion = new DevConsoleCompletion(model);

        List<String> candidates = completion.candidates("set player ");

        assertEquals(List.of("dex", STR_TOKEN), candidates, "Expected every field token, not a re-suggestion of \"player\"");
    }

    @Test
    void acceptingAfterATrailingSpaceAppendsTheCandidateAsANewToken() {
        DevConsoleModel model = new DevConsoleModel(List.of(
                mutatorProviderWithFields(PLAYER_ID, PLAYER_NAME, List.of(STR_TOKEN))
        ));
        DevConsoleCompletion completion = new DevConsoleCompletion(model);

        String filled = completion.apply("set player ", STR_TOKEN);

        assertEquals("set player str", filled);
    }

    private static DevConsoleProvider mutatorProviderWithFields(String id, String localName, List<String> fieldTokens) {
        return new DevConsoleProvider() {
            @Override
            public List<DevConsoleEntry> entries() {
                return List.of(new DevConsoleEntry("core", id, "Player", localName));
            }

            @Override
            public JComponent createPanel(String entryId) {
                return new JPanel();
            }

            @Override
            public Optional<DevConsoleFieldMutator> fieldMutator(String entryId) {
                return Optional.of(new DevConsoleFieldMutator() {
                    @Override
                    public DevConsoleMutationResult apply(DevConsoleMutationVerb verb, String fieldToken, String rawValue) {
                        return new DevConsoleMutationResult.Failure(fieldToken);
                    }

                    @Override
                    public List<String> fieldTokens() {
                        return fieldTokens;
                    }

                    @Override
                    public boolean hasClassDefault(String fieldToken) {
                        return false;
                    }
                });
            }
        };
    }

    private static DevConsoleProvider mutatorProvider(String id, String localName) {
        return mutatorProviderWithFields(id, localName, List.of());
    }

    private static DevConsoleProvider plainProvider(String id, String localName) {
        return new DevConsoleProvider() {
            @Override
            public List<DevConsoleEntry> entries() {
                return List.of(new DevConsoleEntry("core", id, "Classes", localName));
            }

            @Override
            public JComponent createPanel(String entryId) {
                return new JPanel();
            }
        };
    }

    private static DevConsoleProvider snapshotterProvider(String id, String localName) {
        return new DevConsoleProvider() {
            @Override
            public List<DevConsoleEntry> entries() {
                return List.of(new DevConsoleEntry("core", id, "Player", localName));
            }

            @Override
            public JComponent createPanel(String entryId) {
                return new JPanel();
            }

            @Override
            public Optional<DevConsoleSnapshotter> snapshotter(String entryId) {
                return Optional.of(new RecordingSnapshotter());
            }
        };
    }

    private static final class RecordingSnapshotter implements DevConsoleSnapshotter {
        @Override
        public String takeSnapshot(String name) {
            return name;
        }

        @Override
        public Optional<String> restoreSnapshot(String name) {
            return Optional.empty();
        }
    }
}
