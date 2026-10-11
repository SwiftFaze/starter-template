package com.swiftfaze.veil.mods;

import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.entities.dialogue.Dialogue;
import com.swiftfaze.veil.entities.npc.NpcDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Test-only registries holding hand-made content, built through the package-private constructor. */
public final class RegistryFixtures {

    private RegistryFixtures() {
    }

    /** A registry with exactly these buildings (by id) and no tiles, classes, items, quests, themes or NPCs. */
    public static ModRegistry withBuildings(Map<String, Building> buildingsById) {
        ModRegistry.RegistryMaps maps = new ModRegistry.RegistryMaps(
                buildingsById, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        return new ModRegistry(maps, List.of());
    }

    /** A registry with exactly these NPCs (in the given order) and nothing else. */
    public static ModRegistry withNpcs(List<NpcDefinition> npcs) {
        Map<String, NpcDefinition> npcsById = new LinkedHashMap<>();
        for (NpcDefinition npc : npcs) {
            npcsById.put(npc.id(), npc);
        }
        ModRegistry.RegistryMaps maps = new ModRegistry.RegistryMaps(
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), npcsById, Map.of());
        return new ModRegistry(maps, List.of());
    }

    /** A registry with exactly these dialogues (in the given order) and nothing else. */
    public static ModRegistry withDialogues(List<Dialogue> dialogues) {
        Map<String, Dialogue> dialoguesById = new LinkedHashMap<>();
        for (Dialogue dialogue : dialogues) {
            dialoguesById.put(dialogue.id(), dialogue);
        }
        ModRegistry.RegistryMaps maps = new ModRegistry.RegistryMaps(
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), dialoguesById);
        return new ModRegistry(maps, List.of());
    }
}
