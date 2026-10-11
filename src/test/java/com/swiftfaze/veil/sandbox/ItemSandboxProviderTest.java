package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemSandboxProviderTest {

    @Test
    void exposesEveryItemAsAnEntryWithNamespaceAndCategory() {
        ItemSandboxProvider provider = new ItemSandboxProvider();

        List<DevConsoleEntry> entries = provider.entries();

        Optional<DevConsoleEntry> ironSword = entries.stream()
                .filter(entry -> "Iron Sword".equals(entry.name()))
                .findFirst();
        assertTrue(ironSword.isPresent());
        assertEquals("core", ironSword.get().namespace());
        assertEquals("Items", ironSword.get().category());
    }

    @Test
    void createsPanelOpenedToTheRequestedItem() {
        ItemSandboxProvider provider = new ItemSandboxProvider();

        ItemDetailPanel panel = (ItemDetailPanel) provider.createPanel("core:iron_sword");

        assertEquals("Iron Sword", panel.title());
    }

    @Test
    void throwsIllegalArgumentExceptionForUnknownItemId() {
        ItemSandboxProvider provider = new ItemSandboxProvider();

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> provider.createPanel("core:no_such_item"));
        assertTrue(failure.getMessage().contains("Unknown item id"));
    }

    @Test
    void acceptsListConstructorForTesting() {
        ModRegistry mods = ModLoader.load(Paths.get("mods"));
        List<Item> items = mods.getAllItems();
        ItemSandboxProvider provider = new ItemSandboxProvider(items);

        List<DevConsoleEntry> entries = provider.entries();

        assertFalse(entries.isEmpty());
        assertTrue(entries.stream().anyMatch(e -> "Iron Sword".equals(e.name())));
    }

    @Test
    void hasEmptyNamespaceWhenTheColonIsFirst() {
        Item bare = new Item(":bare", "Bare", new Item.ItemAttributes('x', "misc", null, null, List.of()));
        ItemSandboxProvider provider = new ItemSandboxProvider(List.of(bare));

        String namespace = provider.entries().get(0).namespace();

        assertEquals("", namespace);
    }

    @Test
    void usesTheWholeIdAsNamespaceWhenThereIsNoColon() {
        Item plain = new Item("plain", "Plain", new Item.ItemAttributes('x', "misc", null, null, List.of()));
        ItemSandboxProvider provider = new ItemSandboxProvider(List.of(plain));

        String namespace = provider.entries().get(0).namespace();

        assertEquals("plain", namespace);
    }
}
