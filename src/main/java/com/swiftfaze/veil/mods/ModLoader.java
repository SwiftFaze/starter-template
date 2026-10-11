package com.swiftfaze.veil.mods;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.entities.dialogue.Dialogue;
import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.entities.npc.NpcDefinition;
import com.swiftfaze.veil.entities.npc.Patrol;
import com.swiftfaze.veil.entities.npc.PatrolMode;
import com.swiftfaze.veil.entities.npc.TileCoord;
import com.swiftfaze.veil.entities.player.classes.PlayerClass;
import com.swiftfaze.veil.entities.quests.Quest;
import com.swiftfaze.veil.exceptions.ModLoadException;
import com.swiftfaze.veil.world.Tile;
import com.swiftfaze.veil.world.object.InteractableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class ModLoader {
    private static final Comparator<Path> BY_FILE_NAME = Comparator.comparing(path -> path.getFileName().toString());
    public record RegistrationContext<T>(
            Map<String, T> registry,
            Map<String, String> owningModById,
            boolean overrides,
            String contentType
    ) {
    }

    private record RegistryTarget<T>(Map<String, T> registry, Map<String, String> owningModById) {
    }

    private static final Logger logger = LoggerFactory.getLogger(ModLoader.class);

    private ModLoader() {
    }

    public static ModRegistry load(Path modsRoot) {
        List<ModManifest> loadOrder = orderByDependencies(readManifests(modsRoot));

        Map<String, Tile> tilesById = loadAllTiles(modsRoot, loadOrder);

        Map<String, Building> buildingsById = new LinkedHashMap<>();
        Map<String, String> owningBuildingModById = new LinkedHashMap<>();
        Set<String> validStatNames = loadStatRegistry(modsRoot);

        Map<String, PlayerClass> classesById = new LinkedHashMap<>();
        Map<String, String> owningClassModById = new LinkedHashMap<>();
        Map<String, Item> itemsById = new LinkedHashMap<>();
        Map<String, String> owningItemModById = new LinkedHashMap<>();
        Map<String, InteractableObject> objectsById = new LinkedHashMap<>();
        Map<String, String> owningObjectModById = new LinkedHashMap<>();
        Map<String, Quest> questsById = new LinkedHashMap<>();
        Map<String, String> owningQuestModById = new LinkedHashMap<>();
        Map<String, WidgetColorTheme> themesById = new LinkedHashMap<>();
        Map<String, String> owningThemeModById = new LinkedHashMap<>();
        Map<String, NpcDefinition> npcsById = new LinkedHashMap<>();
        Map<String, String> owningNpcModById = new LinkedHashMap<>();
        Map<String, Dialogue> dialoguesById = loadAllDialogues(modsRoot, loadOrder);
        RegistryTarget<Building> buildings = new RegistryTarget<>(buildingsById, owningBuildingModById);
        RegistryTarget<PlayerClass> classes = new RegistryTarget<>(classesById, owningClassModById);
        RegistryTarget<Item> items = new RegistryTarget<>(itemsById, owningItemModById);
        RegistryTarget<InteractableObject> objects = new RegistryTarget<>(objectsById, owningObjectModById);
        RegistryTarget<Quest> quests = new RegistryTarget<>(questsById, owningQuestModById);
        RegistryTarget<NpcDefinition> npcs = new RegistryTarget<>(npcsById, owningNpcModById);
        List<String> modLoadOrder = new ArrayList<>();
        Lookups lookups = new Lookups(tilesById, objectsById);
        for (ModManifest manifest : loadOrder) {
            modLoadOrder.add(manifest.id());
            loadClasses(modsRoot, manifest, validStatNames, classes);
            loadItems(modsRoot, manifest, validStatNames, items);
            loadObjects(modsRoot, manifest, itemsById, objects);
            loadBuildings(modsRoot, manifest, lookups, buildings);
            loadQuests(modsRoot, manifest, itemsById, quests);
            loadThemes(modsRoot, manifest, themesById, owningThemeModById);
            loadNpcs(modsRoot, manifest, npcs, dialoguesById);
        }

        logger.info("Loaded {} mod(s) [{}]: {} tiles, {} buildings, {} classes, {} items, {} objects, {} quests, {} themes, {} NPCs",
                modLoadOrder.size(), String.join(", ", modLoadOrder), tilesById.size(), buildingsById.size(),
                classesById.size(), itemsById.size(), objectsById.size(), questsById.size(), themesById.size(), npcsById.size());

        ModRegistry.RegistryMaps maps = new ModRegistry.RegistryMaps(buildingsById, tilesById, classesById, itemsById, questsById, themesById, objectsById, npcsById, dialoguesById);
        return new ModRegistry(maps, modLoadOrder);
    }

    /** Tiles load first, across every mod, because buildings and objects resolve tile ids against them. */
    private static Map<String, Tile> loadAllTiles(Path modsRoot, List<ModManifest> loadOrder) {
        Map<String, Tile> tilesById = new LinkedHashMap<>();
        Map<String, String> owningTileModById = new LinkedHashMap<>();
        for (ModManifest manifest : loadOrder) {
            loadTiles(modsRoot, manifest, tilesById, owningTileModById);
        }
        return tilesById;
    }

    /**
     * The {@code .json} files directly in {@code dir}, sorted by file name. Directory iteration order
     * is unspecified (NTFS and ext4 differ), so every loader goes through here to load deterministically.
     */
    private static List<Path> jsonFilesIn(Path dir) throws IOException {
        return childrenSortedByName(dir, path -> path.getFileName().toString().endsWith(".json"));
    }

    /** The subdirectories of {@code dir}, sorted by directory name. */
    private static List<Path> subdirectoriesIn(Path dir) throws IOException {
        return childrenSortedByName(dir, Files::isDirectory);
    }

    private static List<Path> childrenSortedByName(Path dir, Predicate<Path> keep) throws IOException {
        try (Stream<Path> children = Files.list(dir)) {
            return children.filter(keep)
                    .sorted(BY_FILE_NAME)
                    .toList();
        }
    }

    private static List<ModManifest> readManifests(Path modsRoot) {
        List<ModManifest> manifests = new ArrayList<>();

        if (!Files.isDirectory(modsRoot)) {
            return manifests;
        }

        try {
            for (Path modDir : subdirectoriesIn(modsRoot)) {
                Path manifestFile = modDir.resolve("mod.json");
                if (Files.exists(manifestFile)) {
                    manifests.add(readManifest(manifestFile));
                }
            }
        } catch (IOException e) {
            throw new ModLoadException("Failed to scan mods directory: " + modsRoot, e);
        }

        return manifests;
    }

    private static ModManifest readManifest(Path manifestFile) {
        try (Reader reader = Files.newBufferedReader(manifestFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            ModSchemaValidator.validate("mod", json, manifestFile);
            String id = json.get("id").getAsString();

            List<String> dependsOn = new ArrayList<>();
            if (json.has("dependsOn")) {
                for (var element : json.getAsJsonArray("dependsOn")) {
                    dependsOn.add(element.getAsString());
                }
            }

            return new ModManifest(id, dependsOn);
        } catch (ModLoadException e) {
            throw e;
        } catch (JsonSyntaxException | IOException e) {
            throw new ModLoadException("Failed to load mod manifest: " + manifestFile, e);
        }
    }

    private static List<ModManifest> orderByDependencies(List<ModManifest> manifests) {
        List<ModManifest> ordered = new ArrayList<>();
        List<ModManifest> remaining = new ArrayList<>(manifests);

        remaining.stream()
                .filter(m -> m.id().equals("core"))
                .findFirst()
                .ifPresent(core -> {
                    ordered.add(core);
                    remaining.remove(core);
                });

        while (!remaining.isEmpty()) {
            List<String> orderedIds = ordered.stream().map(ModManifest::id).toList();

            ModManifest next = remaining.stream()
                    .filter(m -> orderedIds.containsAll(m.dependsOn()))
                    .findFirst()
                    .orElseThrow(() -> new ModLoadException(
                            "Unresolved or cyclic mod dependency among: "
                                    + remaining.stream().map(ModManifest::id).toList()));

            ordered.add(next);
            remaining.remove(next);
        }

        return ordered;
    }

    private static void loadTiles(Path modsRoot, ModManifest manifest,
                                   Map<String, Tile> tilesById,
                                   Map<String, String> owningModById) {
        Path tilesDir = modsRoot.resolve(manifest.id()).resolve("tiles");
        if (!Files.isDirectory(tilesDir)) {
            return;
        }

        try {
            for (Path file : jsonFilesIn(tilesDir)) {
                loadTile(file, manifest.id(), tilesById, owningModById);
            }
        } catch (IOException e) {
            throw new ModLoadException("Failed to scan tiles for mod: " + manifest.id(), e);
        }
    }

    private static void loadTile(Path file, String modId,
                                  Map<String, Tile> tilesById,
                                  Map<String, String> owningModById) {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            ModSchemaValidator.validate("tile", json, file);
            String id = json.get("id").getAsString();
            char symbol = json.get("symbol").getAsString().charAt(0);
            Color color = readColor(json.getAsJsonObject("color"));
            boolean walkable = json.get("walkable").getAsBoolean();

            RegistrationContext<Tile> tileContext = new RegistrationContext<>(tilesById, owningModById, json.has("overrides"), "Tile");
            registerWithCollisionCheck(id, new Tile(id, symbol, color, walkable), modId, tileContext);
        } catch (ModLoadException e) {
            throw e;
        } catch (JsonSyntaxException | IOException e) {
            throw new ModLoadException("Failed to load tile from file: " + file, e);
        }
    }

    static Color readColor(JsonObject color) {
        return new Color(color.get("r").getAsInt(), color.get("g").getAsInt(), color.get("b").getAsInt());
    }

    /** What a building file may refer to by id; live views, filled as mods load. */
    private static final class Lookups {
        private final Map<String, Tile> tilesById;
        private final Map<String, InteractableObject> objectsById;

        Lookups(Map<String, Tile> tilesById, Map<String, InteractableObject> objectsById) {
            this.tilesById = tilesById;
            this.objectsById = objectsById;
        }

        List<List<Tile>> blueprint(JsonObject json, String buildingId, Path file) {
            return readBlueprint(json.getAsJsonArray("tiles"), tilesById, buildingId, file);
        }

        List<Building.ObjectPlacement> placements(JsonObject json, String buildingId, Path file) {
            return ObjectReader.readPlacements(json, buildingId, file, objectsById);
        }
    }

    private static void loadBuildings(Path modsRoot, ModManifest manifest,
                                       Lookups lookups,
                                       RegistryTarget<Building> buildings) {
        Path buildingsDir = modsRoot.resolve(manifest.id()).resolve("buildings");
        if (!Files.isDirectory(buildingsDir)) {
            return;
        }

        try {
            for (Path file : jsonFilesIn(buildingsDir)) {
                loadBuilding(file, manifest.id(), lookups, buildings);
            }
        } catch (IOException e) {
            throw new ModLoadException("Failed to scan buildings for mod: " + manifest.id(), e);
        }
    }

    private static void loadBuilding(Path file, String modId,
                                      Lookups lookups,
                                      RegistryTarget<Building> buildings) {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            ModSchemaValidator.validate("building", json, file);
            String id = json.get("id").getAsString();
            List<List<Tile>> blueprint = lookups.blueprint(json, id, file);
            List<Building.ObjectPlacement> placements = lookups.placements(json, id, file);
            ObjectReader.checkPlacementsInBlueprint(placements, blueprint, id, file);

            RegistrationContext<Building> buildingContext = new RegistrationContext<>(buildings.registry(), buildings.owningModById(), json.has("overrides"), "Building");
            registerWithCollisionCheck(id, new Building(blueprint, placements), modId, buildingContext);
        } catch (ModLoadException e) {
            throw e;
        } catch (JsonSyntaxException | IOException e) {
            throw new ModLoadException("Failed to load building from file: " + file, e);
        }
    }

    private static List<List<Tile>> readBlueprint(JsonArray rows, Map<String, Tile> tilesById, String buildingId, Path file) {
        int height = rows.size();
        int width = rows.get(0).getAsJsonArray().size();
        List<List<Tile>> blueprint = new ArrayList<>(height);

        for (int y = 0; y < height; y++) {
            JsonArray row = rows.get(y).getAsJsonArray();
            List<Tile> tileRow = new ArrayList<>(width);
            for (int x = 0; x < width; x++) {
                String tileId = row.get(x).getAsString();
                Tile tile = tilesById.get(tileId);
                if (tile == null) {
                    throw new ModLoadException("Building '" + buildingId
                            + "' references unknown tile ID: " + tileId + " in file: " + file);
                }
                tileRow.add(tile);
            }
            blueprint.add(tileRow);
        }

        return blueprint;
    }

    private static <T> void registerWithCollisionCheck(String id, T value, String modId,
                                                         RegistrationContext<T> context) {
        if (context.registry().containsKey(id) && !context.overrides()) {
            throw new ModLoadException(context.contentType() + " ID '" + id + "' from mod '" + modId
                    + "' collides with existing content from mod '" + context.owningModById().get(id)
                    + "'; add an \"overrides\" field to confirm this is intentional.");
        }

        if (context.registry().containsKey(id)) {
            logger.info("Mod '{}' overrides {} '{}' previously provided by mod '{}'",
                    modId, context.contentType().toLowerCase(Locale.ROOT), id, context.owningModById().get(id));
        }

        context.registry().put(id, value);
        context.owningModById().put(id, modId);
        logger.debug("Loaded {} '{}' from mod '{}'", context.contentType().toLowerCase(Locale.ROOT), id, modId);
    }

    private static Set<String> loadStatRegistry(Path modsRoot) {
        Path statsFile = modsRoot.resolve("core").resolve("stats.json");
        if (!Files.exists(statsFile)) {
            return Set.of();
        }

        try (Reader reader = Files.newBufferedReader(statsFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            ModSchemaValidator.validate("stats", json, statsFile);
            return parseStatNames(json);
        } catch (ModLoadException e) {
            throw e;
        } catch (JsonSyntaxException | IOException e) {
            throw new ModLoadException("Failed to load stat registry: " + statsFile, e);
        }
    }

    private static Set<String> parseStatNames(JsonObject json) {
        if (!json.has("stats")) {
            return Set.of();
        }
        Set<String> result = new HashSet<>();
        for (var element : json.getAsJsonArray("stats")) {
            result.add(element.getAsString());
        }
        return Set.copyOf(result);
    }

    private static void loadClasses(Path modsRoot, ModManifest manifest,
                                     Set<String> validStatNames,
                                     RegistryTarget<PlayerClass> classes) {
        Path classesDir = modsRoot.resolve(manifest.id()).resolve("classes");
        if (!Files.isDirectory(classesDir)) {
            return;
        }

        try {
            for (Path file : jsonFilesIn(classesDir)) {
                loadClass(file, manifest.id(), validStatNames, classes);
            }
        } catch (IOException e) {
            throw new ModLoadException("Failed to scan classes for mod: " + manifest.id(), e);
        }
    }

    private static void loadClass(Path file, String modId,
                                   Set<String> validStatNames,
                                   RegistryTarget<PlayerClass> classes) {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            ModSchemaValidator.validate("class", json, file);
            String id = json.get("id").getAsString();
            String name = json.get("name").getAsString();
            Map<String, PlayerClass.StatCurve> statsByName = parseClassStats(json, id, validStatNames, file);
            RegistrationContext<PlayerClass> classContext = new RegistrationContext<>(classes.registry(), classes.owningModById(), json.has("overrides"), "PlayerClass");
            registerWithCollisionCheck(id, new PlayerClass(id, name, statsByName), modId, classContext);
        } catch (ModLoadException e) {
            throw e;
        } catch (JsonSyntaxException | IOException e) {
            throw new ModLoadException("Failed to load class from file: " + file, e);
        }
    }

    private static Map<String, PlayerClass.StatCurve> parseClassStats(JsonObject json, String id, Set<String> validStatNames, Path file) {
        Map<String, PlayerClass.StatCurve> statsByName = new LinkedHashMap<>();
        if (json.has("stats")) {
            JsonObject statsObj = json.getAsJsonObject("stats");
            for (String statName : statsObj.keySet()) {
                if (!validStatNames.contains(statName)) {
                    throw new ModLoadException("Class '" + id + "' references unregistered stat '" + statName + "' in file: " + file);
                }
                JsonObject statObj = statsObj.getAsJsonObject(statName);
                int base = statObj.has("base") ? statObj.get("base").getAsInt() : 0;
                String growthCalc = statObj.has("growth") ? statObj.get("growth").getAsString() : null;
                statsByName.put(statName, new PlayerClass.StatCurve(base, growthCalc));
            }
        }
        return statsByName;
    }

    private static void loadItems(Path modsRoot, ModManifest manifest,
                                   Set<String> validStatNames,
                                   RegistryTarget<Item> items) {
        Path itemsDir = modsRoot.resolve(manifest.id()).resolve("items");
        if (!Files.isDirectory(itemsDir)) {
            return;
        }

        try {
            for (Path file : jsonFilesIn(itemsDir)) {
                loadItem(file, manifest.id(), validStatNames, items);
            }
        } catch (IOException e) {
            throw new ModLoadException("Failed to scan items for mod: " + manifest.id(), e);
        }
    }

    private static void loadItem(Path file, String modId,
                                  Set<String> validStatNames,
                                  RegistryTarget<Item> items) {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            ModSchemaValidator.validate("item", json, file);
            String id = json.get("id").getAsString();
            String name = json.get("name").getAsString();
            char glyph = json.get("glyph").getAsString().charAt(0);
            String type = json.get("type").getAsString();
            String slot = json.get("slot").getAsString();
            Item.BaseDamage baseDamage = parseItemBaseDamage(json);
            List<Item.Effect> effects = parseItemEffects(json, id, validStatNames, file);
            Item.ItemAttributes attributes = new Item.ItemAttributes(glyph, type, slot, baseDamage, effects);
            RegistrationContext<Item> itemContext = new RegistrationContext<>(items.registry(), items.owningModById(), json.has("overrides"), "Item");
            registerWithCollisionCheck(id, new Item(id, name, attributes), modId, itemContext);
        } catch (ModLoadException e) {
            throw e;
        } catch (JsonSyntaxException | IOException e) {
            throw new ModLoadException("Failed to load item from file: " + file, e);
        }
    }

    private static void loadObjects(Path modsRoot, ModManifest manifest,
                                     Map<String, Item> itemsById,
                                     RegistryTarget<InteractableObject> objects) {
        Path objectsDir = modsRoot.resolve(manifest.id()).resolve("objects");
        if (!Files.isDirectory(objectsDir)) {
            return;
        }

        try {
            for (Path file : jsonFilesIn(objectsDir)) {
                loadObject(file, manifest.id(), itemsById, objects);
            }
        } catch (IOException e) {
            throw new ModLoadException("Failed to scan objects for mod: " + manifest.id(), e);
        }
    }

    private static void loadObject(Path file, String modId,
                                    Map<String, Item> itemsById,
                                    RegistryTarget<InteractableObject> objects) {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            ModSchemaValidator.validate("object", json, file);
            String id = json.get("id").getAsString();
            InteractableObject object = ObjectReader.readObject(json, id, file, itemsById);
            RegistrationContext<InteractableObject> objectContext = new RegistrationContext<>(objects.registry(), objects.owningModById(), json.has("overrides"), "Object");
            registerWithCollisionCheck(id, object, modId, objectContext);
        } catch (JsonSyntaxException | IOException e) {
            throw new ModLoadException("Failed to load object from file: " + file, e);
        }
    }

    private static Item.BaseDamage parseItemBaseDamage(JsonObject json) {
        if (!json.has("baseDamage")) {
            return new Item.BaseDamage(0, 0);
        }
        JsonObject bd = json.getAsJsonObject("baseDamage");
        int min = bd.has("min") ? bd.get("min").getAsInt() : 0;
        int max = bd.has("max") ? bd.get("max").getAsInt() : 0;
        return new Item.BaseDamage(min, max);
    }

    private static List<Item.Effect> parseItemEffects(JsonObject json, String id, Set<String> validStatNames, Path file) {
        List<Item.Effect> effects = new ArrayList<>();
        if (!json.has("effects")) {
            return effects;
        }
        for (var element : json.getAsJsonArray("effects")) {
            JsonObject effectObj = element.getAsJsonObject();
            String effectType = effectObj.get("type").getAsString();
            String stat = effectObj.get("stat").getAsString();
            String calc = effectObj.get("calc").getAsString();
            if (!validStatNames.contains(stat)) {
                throw new ModLoadException("Item '" + id + "' references unregistered stat '" + stat + "' in file: " + file);
            }
            validateCalcExpression(calc, id, file);
            effects.add(new Item.Effect(effectType, stat, calc));
        }
        return effects;
    }

    private static void validateCalcExpression(String calc, String itemId, Path file) {
        try {
            CalcExpressionParser.evaluate(calc, 0);
        } catch (IllegalArgumentException e) {
            throw new ModLoadException("Item '" + itemId + "' has an invalid calc expression in file: " + file, e);
        }
    }

    private static final Set<String> SUPPORTED_QUEST_OBJECTIVE_TYPES = Set.of("kill");

    private static void loadQuests(Path modsRoot, ModManifest manifest,
                                    Map<String, Item> itemsById,
                                    RegistryTarget<Quest> quests) {
        Path questsDir = modsRoot.resolve(manifest.id()).resolve("quests");
        if (!Files.isDirectory(questsDir)) {
            return;
        }

        try {
            for (Path file : jsonFilesIn(questsDir)) {
                loadQuest(file, manifest.id(), itemsById, quests);
            }
        } catch (IOException e) {
            throw new ModLoadException("Failed to scan quests for mod: " + manifest.id(), e);
        }
    }

    private static void loadQuest(Path file, String modId,
                                   Map<String, Item> itemsById,
                                   RegistryTarget<Quest> quests) {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            ModSchemaValidator.validate("quest", json, file);
            String id = json.get("id").getAsString();
            String name = json.get("name").getAsString();
            Quest.Objective objective = readQuestObjective(json.getAsJsonObject("objective"), id, file);
            List<Quest.Reward> rewards = readQuestRewards(json, id, file, itemsById);

            RegistrationContext<Quest> questContext = new RegistrationContext<>(quests.registry(), quests.owningModById(), json.has("overrides"), "Quest");
            registerWithCollisionCheck(id, new Quest(id, name, objective, rewards), modId, questContext);
        } catch (ModLoadException e) {
            throw e;
        } catch (JsonSyntaxException | IOException e) {
            throw new ModLoadException("Failed to load quest from file: " + file, e);
        }
    }

    private static Quest.Objective readQuestObjective(JsonObject objectiveObj, String questId, Path file) {
        String type = objectiveObj.get("type").getAsString();
        if (!SUPPORTED_QUEST_OBJECTIVE_TYPES.contains(type)) {
            throw new ModLoadException("Quest '" + questId + "' has unsupported objective type '"
                    + type + "' in file: " + file);
        }
        String target = objectiveObj.has("target") ? objectiveObj.get("target").getAsString() : null;
        int count = objectiveObj.has("count") ? objectiveObj.get("count").getAsInt() : 0;
        return new Quest.Objective(type, target, count);
    }

    private static List<Quest.Reward> readQuestRewards(JsonObject json, String questId, Path file,
                                                         Map<String, Item> itemsById) {
        List<Quest.Reward> rewards = new ArrayList<>();
        if (!json.has("rewards")) {
            return rewards;
        }
        for (var element : json.getAsJsonArray("rewards")) {
            rewards.add(readQuestReward(element.getAsJsonObject(), questId, file, itemsById));
        }
        return rewards;
    }

    private static Quest.Reward readQuestReward(JsonObject rewardObj, String questId, Path file,
                                                  Map<String, Item> itemsById) {
        String type = rewardObj.get("type").getAsString();
        return switch (type) {
            case "item" -> parseItemReward(rewardObj, questId, file, itemsById);
            case "xp" -> parseXpReward(rewardObj, questId, file);
            default -> throw new ModLoadException("Quest '" + questId + "' has unsupported reward type '" + type + "' in file: " + file);
        };
    }

    private static Quest.Reward parseItemReward(JsonObject rewardObj, String questId, Path file, Map<String, Item> itemsById) {
        String itemId = rewardObj.get("id").getAsString();
        int count = rewardObj.has("count") ? rewardObj.get("count").getAsInt() : 1;
        if (!itemsById.containsKey(itemId)) {
            throw new ModLoadException("Quest '" + questId + "' references unknown item '" + itemId + "' in file: " + file);
        }
        return new Quest.Reward("item", itemId, count, null);
    }

    private static Quest.Reward parseXpReward(JsonObject rewardObj, String questId, Path file) {
        String calc = rewardObj.get("calc").getAsString();
        try {
            CalcExpressionParser.evaluate(calc, 0);
        } catch (IllegalArgumentException e) {
            throw new ModLoadException("Quest '" + questId + "' has an invalid calc expression in file: " + file, e);
        }
        return new Quest.Reward("xp", null, null, calc);
    }

    private static void loadThemes(Path modsRoot, ModManifest manifest,
                                    Map<String, WidgetColorTheme> themesById,
                                    Map<String, String> owningModById) {
        Path themesDir = modsRoot.resolve(manifest.id()).resolve("themes");
        if (!Files.isDirectory(themesDir)) {
            return;
        }

        try {
            for (Path file : jsonFilesIn(themesDir)) {
                loadTheme(file, manifest.id(), themesById, owningModById);
            }
        } catch (IOException e) {
            throw new ModLoadException("Failed to scan themes for mod: " + manifest.id(), e);
        }
    }

    private static void loadTheme(Path file, String modId,
                                   Map<String, WidgetColorTheme> themesById,
                                   Map<String, String> owningModById) {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            ModSchemaValidator.validate("theme", json, file);
            String id = json.get("id").getAsString();
            Map<String, Color> colorsByKey = readThemeColors(json.getAsJsonObject("colors"), id, file);

            RegistrationContext<WidgetColorTheme> themeContext = new RegistrationContext<>(themesById, owningModById, json.has("overrides"), "Theme");
            registerWithCollisionCheck(id, new WidgetColorTheme(id, colorsByKey), modId, themeContext);
        } catch (ModLoadException e) {
            throw e;
        } catch (JsonSyntaxException | IOException e) {
            throw new ModLoadException("Failed to load theme from file: " + file, e);
        }
    }

    private static Map<String, Color> readThemeColors(JsonObject colorsJson, String themeId, Path file) {
        Map<String, Color> colorsByKey = new LinkedHashMap<>();
        for (String key : WidgetColorTheme.REQUIRED_KEYS) {
            if (colorsJson == null || !colorsJson.has(key)) {
                throw new ModLoadException("Theme '" + themeId + "' is missing required color key '"
                        + key + "' in file: " + file);
            }
            colorsByKey.put(key, readColor(colorsJson.getAsJsonObject(key)));
        }
        return colorsByKey;
    }

    /** Dialogues load before any NPC, across every mod, so an NPC can name a dialogue from any mod. */
    private static Map<String, Dialogue> loadAllDialogues(Path modsRoot, List<ModManifest> loadOrder) {
        RegistryTarget<Dialogue> dialogues = new RegistryTarget<>(new LinkedHashMap<>(), new LinkedHashMap<>());
        for (ModManifest manifest : loadOrder) {
            loadDialogues(modsRoot, manifest, dialogues);
        }
        return dialogues.registry();
    }

    private static void loadDialogues(Path modsRoot, ModManifest manifest, RegistryTarget<Dialogue> dialogues) {
        Path dialoguesDir = modsRoot.resolve(manifest.id()).resolve("dialogues");
        if (!Files.isDirectory(dialoguesDir)) {
            return;
        }

        try {
            for (Path file : jsonFilesIn(dialoguesDir)) {
                loadDialogue(file, manifest.id(), dialogues);
            }
        } catch (IOException e) {
            throw new ModLoadException("Failed to scan dialogues for mod: " + manifest.id(), e);
        }
    }

    private static void loadDialogue(Path file, String modId, RegistryTarget<Dialogue> dialogues) {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            validateDialogue(json, file);
            String id = json.get("id").getAsString();
            Dialogue dialogue = DialogueReader.read(json, id, file);
            RegistrationContext<Dialogue> dialogueContext = new RegistrationContext<>(dialogues.registry(), dialogues.owningModById(), json.has("overrides"), "Dialogue");
            registerWithCollisionCheck(id, dialogue, modId, dialogueContext);
        } catch (JsonSyntaxException | IOException e) {
            throw new ModLoadException("Failed to load dialogue from file: " + file, e);
        }
    }

    /** Schema failures are re-thrown with the dialogue id prepended, naming the dialogue and the file. */
    private static void validateDialogue(JsonObject json, Path file) {
        try {
            ModSchemaValidator.validate("dialogue", json, file);
        } catch (ModLoadException e) {
            String id = json.has("id") ? json.get("id").toString() : "(unknown)";
            throw new ModLoadException("Dialogue " + id + " is invalid: " + e.getMessage(), e);
        }
    }

    private static void loadNpcs(Path modsRoot, ModManifest manifest, RegistryTarget<NpcDefinition> npcs,
                                 Map<String, Dialogue> dialoguesById) {
        Path npcsDir = modsRoot.resolve(manifest.id()).resolve("npcs");
        if (!Files.isDirectory(npcsDir)) {
            return;
        }

        try {
            for (Path file : jsonFilesIn(npcsDir)) {
                loadNpc(file, manifest.id(), npcs, dialoguesById);
            }
        } catch (IOException e) {
            throw new ModLoadException("Failed to scan npcs for mod: " + manifest.id(), e);
        }
    }

    private static void loadNpc(Path file, String modId, RegistryTarget<NpcDefinition> npcs,
                                Map<String, Dialogue> dialoguesById) {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            validateNpc(json, file);
            String id = json.get("id").getAsString();
            NpcDefinition definition = readNpcDefinition(json, id, file, dialoguesById);
            RegistrationContext<NpcDefinition> npcContext = new RegistrationContext<>(npcs.registry(), npcs.owningModById(), json.has("overrides"), "NPC");
            registerWithCollisionCheck(id, definition, modId, npcContext);
        } catch (JsonSyntaxException | IOException e) {
            throw new ModLoadException("Failed to load NPC from file: " + file, e);
        }
    }

    /** Schema failures are re-thrown with the NPC id prepended, naming the NPC as well as the file. */
    private static void validateNpc(JsonObject json, Path file) {
        try {
            ModSchemaValidator.validate("npc", json, file);
        } catch (ModLoadException e) {
            String id = json.has("id") ? json.get("id").toString() : "(unknown)";
            throw new ModLoadException("NPC " + id + " is invalid: " + e.getMessage(), e);
        }
    }

    private static NpcDefinition readNpcDefinition(JsonObject json, String id, Path file,
                                                   Map<String, Dialogue> dialoguesById) {
        String name = json.get("name").getAsString();
        char symbol = json.get("symbol").getAsString().charAt(0);
        Color color = readColor(json.getAsJsonObject("color"));
        TileCoord spawn = readTileCoord(json.getAsJsonObject("spawn"));
        Patrol patrol = json.has("patrol") ? readPatrol(json.getAsJsonObject("patrol"), id, spawn, file) : null;
        Dialogue dialogue = json.has("dialogue")
                ? requireDialogue(json.get("dialogue").getAsString(), id, file, dialoguesById)
                : null;
        return new NpcDefinition(id, name, symbol, color, spawn, patrol, dialogue);
    }

    private static Dialogue requireDialogue(String dialogueId, String npcId, Path file, Map<String, Dialogue> dialoguesById) {
        Dialogue dialogue = dialoguesById.get(dialogueId);
        if (dialogue == null) {
            throw new ModLoadException("NPC '" + npcId + "' refers to unknown dialogue '" + dialogueId + "' in file: " + file);
        }
        return dialogue;
    }

    private static TileCoord readTileCoord(JsonObject tile) {
        return new TileCoord(tile.get("x").getAsInt(), tile.get("y").getAsInt());
    }

    private static Patrol readPatrol(JsonObject patrolObj, String npcId, TileCoord spawn, Path file) {
        String mode = patrolObj.has("mode") ? patrolObj.get("mode").getAsString() : "ping_pong";
        PatrolMode patrolMode = PatrolMode.fromJson(mode);
        List<TileCoord> waypoints = new ArrayList<>();
        for (JsonElement element : patrolObj.getAsJsonArray("waypoints")) {
            waypoints.add(readTileCoord(element.getAsJsonObject()));
        }
        validateRoute(npcId, spawn, waypoints, file);
        if (patrolMode == PatrolMode.LOOP) {
            requireOrthogonalLeg(npcId, waypoints.get(waypoints.size() - 1), waypoints.get(0), file);
        }
        return new Patrol(patrolMode, waypoints);
    }

    /** The route must start on the spawn tile, and each waypoint-to-waypoint leg must be straight. */
    private static void validateRoute(String npcId, TileCoord spawn, List<TileCoord> waypoints, Path file) {
        if (!waypoints.get(0).equals(spawn)) {
            throw new ModLoadException("NPC '" + npcId + "' patrol must start at its spawn " + describe(spawn)
                    + " but its first waypoint is " + describe(waypoints.get(0)) + " in file: " + file);
        }
        for (int i = 0; i + 1 < waypoints.size(); i++) {
            requireOrthogonalLeg(npcId, waypoints.get(i), waypoints.get(i + 1), file);
        }
    }

    private static void requireOrthogonalLeg(String npcId, TileCoord start, TileCoord end, Path file) {
        boolean sameColumn = start.x() == end.x();
        boolean sameRow = start.y() == end.y();
        if (sameColumn == sameRow) {
            throw new ModLoadException("NPC '" + npcId + "' patrol leg from " + describe(start) + " to " + describe(end)
                    + " must be a straight horizontal or vertical line in file: " + file);
        }
    }

    private static String describe(TileCoord tile) {
        return "(" + tile.x() + ", " + tile.y() + ")";
    }

    private record ModManifest(String id, List<String> dependsOn) {
    }
}
