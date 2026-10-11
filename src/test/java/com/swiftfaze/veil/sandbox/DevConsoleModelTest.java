package com.swiftfaze.veil.sandbox;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.swiftfaze.veil.exceptions.ModLoadException;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevConsoleModelTest {

    @Test
    void filtersEntriesBySubstringCaseInsensitively() {
        DevConsoleModel model = new DevConsoleModel(List.of(stubProvider("Classes", "Mage", "Warrior")));

        model.setSearchText("MAG");

        List<String> names = model.filteredResults().stream().map(r -> r.entry().name()).toList();
        assertEquals(List.of("Mage"), names);
    }

    @Test
    void emptySearchTextIncludesAllEntries() {
        DevConsoleModel model = new DevConsoleModel(List.of(stubProvider("Classes", "Mage", "Warrior")));

        assertEquals(2, model.filteredResults().size());
    }

    @Test
    void noMatchesReturnsEmptyList() {
        DevConsoleModel model = new DevConsoleModel(List.of(stubProvider("Classes", "Mage")));

        model.setSearchText("zzz");

        assertTrue(model.filteredResults().isEmpty());
    }

    @Test
    void matchesOnCategoryNotJustName() {
        DevConsoleModel model = new DevConsoleModel(List.of(stubProvider("Classes", "Mage", "Warrior")));

        model.setSearchText("class");

        assertEquals(2, model.filteredResults().size());
    }

    @Test
    void matchesOnNamespaceNotJustName() {
        DevConsoleModel model = new DevConsoleModel(List.of(stubProvider("Classes", "Mage", "Warrior")));

        model.setSearchText("core");

        assertEquals(2, model.filteredResults().size());
    }

    @Test
    void flattensEntriesAcrossMultipleProviders() {
        DevConsoleModel model = new DevConsoleModel(List.of(
                stubProvider("Classes", "Mage"),
                stubProvider("Quests", "Goblin Slayer")
        ));

        assertEquals(2, model.filteredResults().size());
    }

    @Test
    void findsEntryByFullyQualifiedId() {
        DevConsoleModel model = new DevConsoleModel(List.of(stubProvider("Classes", "Mage", "Warrior")));
        assertTrue(model.findById("core:mage").isPresent());
        assertEquals("Mage", model.findById("core:mage").get().entry().name());
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        DevConsoleModel model = new DevConsoleModel(List.of(stubProvider("Classes", "Mage")));
        assertTrue(model.findById("core:ghost").isEmpty());
    }

    @Test
    void reloadRebuildsResultsWithUpdatedEntries() {
        ReloadableFakeProvider provider = new ReloadableFakeProvider("Classes", "Mage");
        DevConsoleModel model = new DevConsoleModel(List.of(provider));

        provider.addEntry(new DevConsoleEntry("core", "core:rogue", "Classes", "Rogue"));
        int count = model.reload();

        assertEquals(2, count);
        assertEquals(2, model.allResults().size());
    }

    @Test
    void reloadMakesNewlyAddedEntryFindable() {
        ReloadableFakeProvider provider = new ReloadableFakeProvider("Classes", "Mage");
        DevConsoleModel model = new DevConsoleModel(List.of(provider));

        provider.addEntry(new DevConsoleEntry("core", "core:rogue", "Classes", "Rogue"));
        model.reload();

        assertTrue(model.findById("core:rogue").isPresent());
    }

    @Test
    void reloadReturnsNewEntryCount() {
        ReloadableFakeProvider provider = new ReloadableFakeProvider("Classes", "Mage", "Warrior");
        DevConsoleModel model = new DevConsoleModel(List.of(provider));
        provider.removeEntryById("core:warrior");

        int count = model.reload();

        assertEquals(1, count);
    }

    @Test
    void reloadKeepsPreviousEntriesOnException() {
        ReloadableFakeProvider provider = new ReloadableFakeProvider("Classes", "Mage");
        DevConsoleModel model = new DevConsoleModel(List.of(provider));

        provider.setNextReloadFails("Failed to load tile from file: bad.json");
        assertThrows(ModLoadException.class, model::reload);

        assertEquals(1, model.allResults().size());
        assertTrue(model.findById("core:mage").isPresent());
    }

    @Test
    void reloadCallsEveryProviderOnce() {
        ReloadableFakeProvider provider1 = new ReloadableFakeProvider("Classes", "Mage");
        ReloadableFakeProvider provider2 = new ReloadableFakeProvider("Quests", "Quest1");
        DevConsoleModel model = new DevConsoleModel(List.of(provider1, provider2));

        model.reload();

        assertEquals(1, provider1.reloadCallCount());
        assertEquals(1, provider2.reloadCallCount());
    }

    private static DevConsoleProvider stubProvider(String category, String... names) {
        return new DevConsoleProvider() {
            @Override
            public List<DevConsoleEntry> entries() {
                return List.of(names).stream()
                        .map(name -> new DevConsoleEntry("core", "core:" + name.toLowerCase(Locale.ROOT), category, name))
                        .toList();
            }

            @Override
            public JComponent createPanel(String id) {
                return new JPanel();
            }
        };
    }
}
