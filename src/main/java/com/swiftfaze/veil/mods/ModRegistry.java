package com.swiftfaze.veil.mods;

import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.entities.dialogue.Dialogue;
import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.entities.npc.NpcDefinition;
import com.swiftfaze.veil.entities.player.classes.PlayerClass;
import com.swiftfaze.veil.entities.quests.Quest;
import com.swiftfaze.veil.world.Tile;
import com.swiftfaze.veil.world.object.InteractableObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModRegistry {
    private final Map<String, Building> buildingsById = new LinkedHashMap<>();
    private final Map<String, Tile> tilesById = new LinkedHashMap<>();
    private final Map<String, PlayerClass> classesById = new LinkedHashMap<>();
    private final Map<String, Item> itemsById = new LinkedHashMap<>();
    private final Map<String, Quest> questsById = new LinkedHashMap<>();
    private final Map<String, WidgetColorTheme> themesById = new LinkedHashMap<>();
    private final Map<String, InteractableObject> objectsById = new LinkedHashMap<>();
    private final Map<String, NpcDefinition> npcsById = new LinkedHashMap<>();
    private final Map<String, Dialogue> dialoguesById = new LinkedHashMap<>();
    private final List<String> modLoadOrder;

    public record RegistryMaps(
            Map<String, Building> buildingsById,
            Map<String, Tile> tilesById,
            Map<String, PlayerClass> classesById,
            Map<String, Item> itemsById,
            Map<String, Quest> questsById,
            Map<String, WidgetColorTheme> themesById,
            Map<String, InteractableObject> objectsById,
            Map<String, NpcDefinition> npcsById,
            Map<String, Dialogue> dialoguesById
    ) {
        public RegistryMaps {
            buildingsById = copyOfOrdered(buildingsById);
            tilesById = copyOfOrdered(tilesById);
            classesById = copyOfOrdered(classesById);
            itemsById = copyOfOrdered(itemsById);
            questsById = copyOfOrdered(questsById);
            themesById = copyOfOrdered(themesById);
            objectsById = copyOfOrdered(objectsById);
            npcsById = copyOfOrdered(npcsById);
            dialoguesById = copyOfOrdered(dialoguesById);
        }
    }


    private static <V> Map<String, V> copyOfOrdered(Map<String, V> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    ModRegistry(RegistryMaps maps, List<String> modLoadOrder) {
        this.buildingsById.putAll(maps.buildingsById());
        this.tilesById.putAll(maps.tilesById());
        this.classesById.putAll(maps.classesById());
        this.itemsById.putAll(maps.itemsById());
        this.questsById.putAll(maps.questsById());
        this.themesById.putAll(maps.themesById());
        this.objectsById.putAll(maps.objectsById());
        this.npcsById.putAll(maps.npcsById());
        this.dialoguesById.putAll(maps.dialoguesById());
        this.modLoadOrder = List.copyOf(modLoadOrder);
    }

    public NpcDefinition getNpc(String id) {
        return npcsById.get(id);
    }

    public List<NpcDefinition> getAllNpcs() {
        return List.copyOf(npcsById.values());
    }

    public Dialogue getDialogue(String id) {
        return dialoguesById.get(id);
    }

    public List<Dialogue> getAllDialogues() {
        return List.copyOf(dialoguesById.values());
    }

    public Building getBuilding(String id) {
        return buildingsById.get(id);
    }

    /** Every registered building id, sorted, so choices don't depend on mod load order. */
    public List<String> getBuildingIds() {
        return buildingsById.keySet().stream().sorted().toList();
    }

    public Tile getTile(String id) {
        return tilesById.get(id);
    }

    public List<Tile> getAllTiles() {
        return List.copyOf(tilesById.values());
    }

    public PlayerClass getPlayerClass(String id) {
        return classesById.get(id);
    }

    public List<PlayerClass> getAllPlayerClasses() {
        return List.copyOf(classesById.values());
    }

    public Item getItem(String id) {
        return itemsById.get(id);
    }

    public List<Item> getAllItems() {
        return List.copyOf(itemsById.values());
    }

    public Quest getQuest(String id) {
        return questsById.get(id);
    }

    public List<Quest> getAllQuests() {
        return List.copyOf(questsById.values());
    }

    public WidgetColorTheme getTheme(String id) {
        return themesById.get(id);
    }

    public List<WidgetColorTheme> getAllThemes() {
        return List.copyOf(themesById.values());
    }

    /** A template to copy with {@link InteractableObject#newInstance()} before placing it. */
    public InteractableObject getObject(String id) {
        return objectsById.get(id);
    }

    public List<String> getModLoadOrder() {
        return modLoadOrder;
    }
}
