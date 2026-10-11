package com.swiftfaze.veil.sandbox;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassSandboxProviderTest {

    @Test
    void exposesEveryClassAsAnEntryWithNamespaceAndCategory() {
        ClassSandboxProvider provider = new ClassSandboxProvider();

        List<DevConsoleEntry> entries = provider.entries();

        Optional<DevConsoleEntry> warrior = entries.stream()
                .filter(entry -> entry.name().equals("Warrior"))
                .findFirst();
        assertTrue(warrior.isPresent());
        assertEquals("core", warrior.get().namespace());
        assertEquals("Classes", warrior.get().category());
    }

    @Test
    void createsPanelOpenedToTheRequestedClass() {
        ClassSandboxProvider provider = new ClassSandboxProvider();

        ClassDetailPanel panel = (ClassDetailPanel) provider.createPanel("core:mage");

        assertEquals("Mage", panel.getHeader().getTitle());
        assertEquals(List.of("Attack Power", "16", "16", "16", "16", "16"), panel.getStatsTable().getSelectedRow());
    }
}
