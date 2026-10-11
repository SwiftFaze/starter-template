package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.mods.ModLoader;

import javax.swing.JComponent;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Exposes every mod-loaded item as its own searchable dev-console entry,
 * reusing the Inspectable / DetailTable output already used by InventoryPanel
 * and CodexPanel. Supports live editing of base damage fields via
 * {@link #fieldMutator(String)}, which swaps in a new immutable Item with the
 * edited damage values into an id-keyed map. The original mod-loaded BaseDamage
 * values are cached separately to support the "default" keyword restoration.
 */
public class ItemSandboxProvider implements DevConsoleProvider {

    private static final String CATEGORY = "Items";

    private final Map<String, Item> itemsById;
    private final Map<String, Item.BaseDamage> originalBaseDamageById;

    public ItemSandboxProvider() {
        this(ModLoader.load(Paths.get("mods")).getAllItems());
    }

    public ItemSandboxProvider(Collection<Item> items) {
        this.itemsById = new LinkedHashMap<>();
        this.originalBaseDamageById = new LinkedHashMap<>();
        for (Item item : items) {
            this.itemsById.put(item.getId(), item);
            this.originalBaseDamageById.put(item.getId(), item.getBaseDamage());
        }
    }

    @Override
    public List<DevConsoleEntry> entries() {
        return itemsById.values().stream()
                .map(item -> new DevConsoleEntry(
                        namespaceOf(item.getId()),
                        item.getId(),
                        CATEGORY,
                        item.getName()))
                .toList();
    }

    @Override
    public JComponent createPanel(String id) {
        return new ItemDetailPanel(findItemById(id), itemFieldMutator(id));
    }

    @Override
    public Optional<DevConsoleFieldMutator> fieldMutator(String id) {
        findItemById(id);
        return Optional.of(itemFieldMutator(id));
    }

    private ItemFieldMutator itemFieldMutator(String id) {
        return new ItemFieldMutator(
                originalBaseDamageById.get(id),
                () -> findItemById(id),
                updated -> itemsById.put(id, updated));
    }

    private Item findItemById(String id) {
        Item item = itemsById.get(id);
        if (item == null) {
            throw new IllegalArgumentException("Unknown item id: " + id);
        }
        return item;
    }

    private static String namespaceOf(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(0, colon) : id;
    }
}
