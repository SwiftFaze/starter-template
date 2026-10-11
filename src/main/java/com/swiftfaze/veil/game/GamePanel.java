package com.swiftfaze.veil.game;

import com.swiftfaze.veil.GameConst;
import com.swiftfaze.veil.render.Camera;
import com.swiftfaze.veil.render.DrawableAsciiEntity;
import com.swiftfaze.veil.render.Positionable;
import com.swiftfaze.veil.entities.dialogue.Conversation;
import com.swiftfaze.veil.entities.npc.NonPlayerCharacter;
import com.swiftfaze.veil.entities.npc.NpcDefinition;
import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.game.event.GameEvent;
import com.swiftfaze.veil.game.event.GameEventLog;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.game.worldgen.GeneratedWorldScene;
import com.swiftfaze.veil.mods.ModRegistry;
import com.swiftfaze.veil.world.WorldScene;
import com.swiftfaze.veil.world.WorldSeeds;
import com.swiftfaze.veil.world.object.Chest;
import com.swiftfaze.veil.world.object.InteractableObject;
import com.swiftfaze.veil.world.object.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.LongSupplier;


public final class GamePanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(GamePanel.class);

    private Player player;
    private WorldScene scene;
    private final LongSupplier worldSeeds;
    private final Camera camera = new Camera(GameConst.GAME_WINDOW_WIDTH, GameConst.GAME_WINDOW_HEIGHT);
    private final List<Positionable> entitiesToDraw = new ArrayList<>();
    private final List<GameListener> listeners = new ArrayList<>();
    private final GameEventLog eventLog;
    private final WorldPause worldPause = new WorldPause();
    private final TurnScheduler turnScheduler = new TurnScheduler();
    private final List<NonPlayerCharacter> npcs = new ArrayList<>();
    /** The definitions last passed to {@link #spawnNpcs}; regenerateWorld respawns from these. */
    private List<NpcDefinition> npcDefinitions = List.of();
    private static final String BLOCKED_MESSAGE = "Something is in the way.";
    // Interact range: the four orthogonal neighbours, in this order; the first object found wins.
    private static final List<Offset> INTERACT_OFFSETS = List.of(new Offset(0, -1), new Offset(1, 0), new Offset(0, 1), new Offset(-1, 0));

    /** What a new game starts with: the player on the scene's spawn point, and the scene. */
    private record NewGame(Player player, GeneratedWorldScene scene) {
    }

    public GamePanel() {
        this(GameEventLog.noOp());
    }

    public GamePanel(GameEventLog eventLog) {
        this(eventLog, WorldSeeds.fromSystemProperty(new Random()::nextLong));
    }

    /** @param worldSeeds supplies the seed for each new game (and each {@link #resetState()}) */
    public GamePanel(GameEventLog eventLog, LongSupplier worldSeeds) {
        this(eventLog, worldSeeds, newGame(worldSeeds));
    }

    public GamePanel(GameEventLog eventLog, Player player, WorldScene scene) {
        this(eventLog, WorldSeeds.fromSystemProperty(new Random()::nextLong), player, scene);
    }

    private GamePanel(GameEventLog eventLog, LongSupplier worldSeeds, NewGame game) {
        this(eventLog, worldSeeds, game.player(), game.scene());
    }

    private GamePanel(GameEventLog eventLog, LongSupplier worldSeeds, Player player, WorldScene scene) {
        this.player = player;
        this.scene = scene;
        this.worldSeeds = worldSeeds;
        this.eventLog = eventLog;
        setPreferredSize(new Dimension(GameConst.GAME_WINDOW_WIDTH * GameConst.TILE_WIDTH, GameConst.GAME_WINDOW_HEIGHT * GameConst.TILE_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        addEntity(scene);
        addEntity(player);

        bindKeys();
    }

    private void bindKeys() {
        InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        inputMap.put(Keybindings.MOVE_UP_Z, Keybindings.ACTION_MOVE_UP);
        inputMap.put(Keybindings.MOVE_UP_ARROW, Keybindings.ACTION_MOVE_UP);
        inputMap.put(Keybindings.MOVE_DOWN_S, Keybindings.ACTION_MOVE_DOWN);
        inputMap.put(Keybindings.MOVE_DOWN_ARROW, Keybindings.ACTION_MOVE_DOWN);
        inputMap.put(Keybindings.MOVE_LEFT_Q, Keybindings.ACTION_MOVE_LEFT);
        inputMap.put(Keybindings.MOVE_LEFT_ARROW, Keybindings.ACTION_MOVE_LEFT);
        inputMap.put(Keybindings.MOVE_RIGHT_D, Keybindings.ACTION_MOVE_RIGHT);
        inputMap.put(Keybindings.MOVE_RIGHT_ARROW, Keybindings.ACTION_MOVE_RIGHT);
        inputMap.put(Keybindings.TOGGLE_INVENTORY, Keybindings.ACTION_TOGGLE_INVENTORY);
        inputMap.put(Keybindings.TOGGLE_CODEX, Keybindings.ACTION_TOGGLE_CODEX);
        inputMap.put(Keybindings.TOGGLE_DEV_CONSOLE, Keybindings.ACTION_TOGGLE_DEV_CONSOLE);
        inputMap.put(Keybindings.INTERACT, Keybindings.ACTION_INTERACT);
        inputMap.put(Keybindings.MENU_CANCEL, Keybindings.ACTION_TOGGLE_PAUSE);

        actionMap.put(Keybindings.ACTION_MOVE_UP, new MoveAction(worldScene -> player.moveUp(worldScene)));
        actionMap.put(Keybindings.ACTION_MOVE_DOWN, new MoveAction(worldScene -> player.moveDown(worldScene)));
        actionMap.put(Keybindings.ACTION_MOVE_LEFT, new MoveAction(worldScene -> player.moveLeft(worldScene)));
        actionMap.put(Keybindings.ACTION_MOVE_RIGHT, new MoveAction(worldScene -> player.moveRight(worldScene)));
        actionMap.put(Keybindings.ACTION_TOGGLE_INVENTORY, new ToggleInventoryAction());
        actionMap.put(Keybindings.ACTION_TOGGLE_CODEX, new ToggleCodexAction());
        actionMap.put(Keybindings.ACTION_TOGGLE_DEV_CONSOLE, new ToggleDevConsoleAction());
        actionMap.put(Keybindings.ACTION_INTERACT, new InteractAction());
        actionMap.put(Keybindings.ACTION_TOGGLE_PAUSE, new TogglePauseAction());
    }

    private void notifyPlayerUpdated() {
        for (GameListener l : listeners) {
            l.updatePlayer(player);
        }
        repaint();
    }

    public void addEntity(Positionable entity) {
        entitiesToDraw.add(entity);
    }

    /** Spawns every NPC in {@code registry}, in load order. Call at setup and after resetState(). */
    public void spawnNpcs(ModRegistry registry) {
        npcDefinitions = registry.getAllNpcs();
        for (NpcDefinition definition : npcDefinitions) {
            spawnNpc(definition);
        }
    }

    /** Spawns one NPC: draws it, and registers it with the turn scheduler after any earlier NPCs. */
    public void spawnNpc(NpcDefinition definition) {
        NonPlayerCharacter npc = new NonPlayerCharacter(definition, this::canNpcEnter);
        npcs.add(npc);
        addEntity(npc);
        turnScheduler.register(npc::onTurn);
    }

    public List<NonPlayerCharacter> getNpcs() {
        return List.copyOf(npcs);
    }

    public List<Positionable> getEntitiesToDraw() {
        return List.copyOf(entitiesToDraw);
    }

    /** NPCs may step on a walkable tile that holds neither the player nor another NPC. */
    private boolean canNpcEnter(int x, int y) {
        boolean occupiedByPlayer = player.getX() == x && player.getY() == y;
        boolean occupiedByNpc = npcs.stream().anyMatch(npc -> npc.getX() == x && npc.getY() == y);
        return scene.isWalkable(x, y) && !occupiedByPlayer && !occupiedByNpc;
    }

    public void startGameLoop() {
        requestFocusInWindow();
    }

    public void addGameListener(GameListener listener) {
        listeners.add(listener);
    }

    public Player getPlayer() {
        return player;
    }

    public WorldScene getScene() {
        return scene;
    }

    /** Builds a fresh world from the next seed and spawns the player on its spawn point. */
    private static NewGame newGame(LongSupplier worldSeeds) {
        GeneratedWorldScene scene = generateWorld(worldSeeds.getAsLong());
        return new NewGame(new Player(scene.getSpawnX(), scene.getSpawnY()), scene);
    }

    private static GeneratedWorldScene generateWorld(long seed) {
        GeneratedWorldScene scene = new GeneratedWorldScene(GameConst.DEFAULT_MAP_WIDTH, GameConst.DEFAULT_MAP_HEIGHT, seed);
        logger.info("World generated from seed {}", seed);
        return scene;
    }

    /**
     * Replaces the running world with one generated from {@code seed}. The same Player stays
     * (stats, class, inventory untouched) and moves to the new spawn point; pause state is kept.
     * NPCs are respawned from the last {@link #spawnNpcs} definitions at their spawn points,
     * and the old NPCs are unregistered from the turn scheduler. The turn counter is kept.
     */
    public void regenerateWorld(long seed) {
        GeneratedWorldScene regenerated = generateWorld(seed);
        entitiesToDraw.set(entitiesToDraw.indexOf(scene), regenerated);
        scene = regenerated;
        player.setPosition(regenerated.getSpawnX(), regenerated.getSpawnY());
        respawnNpcs();
        notifyPlayerUpdated();
    }

    /** Drops every NPC from the draw list and the scheduler, then spawns the remembered definitions afresh. */
    private void respawnNpcs() {
        entitiesToDraw.removeAll(npcs);
        npcs.clear();
        turnScheduler.clearRegistrations();
        for (NpcDefinition definition : npcDefinitions) {
            spawnNpc(definition);
        }
    }

    public WorldPause getWorldPause() {
        return worldPause;
    }

    public TurnScheduler getTurnScheduler() {
        return turnScheduler;
    }

    public boolean isPaused() {
        return worldPause.isPaused();
    }

    /**
     * Resets Player/WorldScene state back to a fresh session, so a subsequent
     * New/Continue doesn't inherit stale state from an exited game (e.g. via
     * the pause menu's Exit to Main Menu). Replaces the old dispose-and-
     * reload-everything approach (removed with the F5 hot-reset feature) with
     * an in-place reset of just this panel's own state.
     */
    public void resetState() {
        NewGame game = newGame(worldSeeds);
        player = game.player();
        scene = game.scene();
        entitiesToDraw.clear();
        npcs.clear();
        addEntity(scene);
        addEntity(player);
        turnScheduler.reset();
        worldPause.releaseAll();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        camera.resizeViewport(getWidth() / GameConst.TILE_WIDTH, getHeight() / GameConst.TILE_HEIGHT);
        camera.centerOn(player.getX(), player.getY());

        scene.renderWorld(g2d, GameConst.TILE_WIDTH, GameConst.TILE_HEIGHT, camera);

        for (Positionable entity : entitiesToDraw) {
            if (Objects.equals(entity, scene)) {
                continue;
            }

            if (entity instanceof DrawableAsciiEntity ascii) {
                ascii.render(g2d, GameConst.TILE_WIDTH, GameConst.TILE_HEIGHT, camera);
            }
        }
    }

    private record Offset(int dx, int dy) {
    }

    private final class InteractAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (worldPause.isPaused()) {
                return;
            }
            for (Offset offset : INTERACT_OFFSETS) {
                int x = player.getX() + offset.dx();
                int y = player.getY() + offset.dy();
                Optional<Conversation> conversation = conversationAt(x, y);
                if (conversation.isPresent()) {
                    openConversation(conversation.get());
                    return;
                }
                InteractableObject object = scene.getObject(x, y);
                if (object != null) {
                    interactWith(x, y, object);
                    return;
                }
            }
        }
    }

    /** A new conversation with the NPC on (x, y), if it has a dialogue; an NPC without one is skipped. */
    private Optional<Conversation> conversationAt(int x, int y) {
        return npcs.stream()
                .filter(npc -> npc.getX() == x && npc.getY() == y && npc.getDialogue() != null)
                .findFirst()
                .map(npc -> new Conversation(npc.getName(), npc.getDialogue()));
    }

    private void openConversation(Conversation conversation) {
        for (GameListener l : listeners) {
            l.openDialogue(conversation);
        }
        repaint();
    }

    private void interactWith(int x, int y, InteractableObject object) {
        InteractionResult result = object.interact(isOccupied(x, y));
        if (result == InteractionResult.REFUSED) {
            eventLog.recordEvent(GameEvent.interactionRefused(BLOCKED_MESSAGE));
        } else if (result == InteractionResult.SHOW_CONTENTS && object instanceof Chest chest) {
            for (GameListener l : listeners) {
                l.openChest(chest);
            }
        }
        repaint();
    }

    /** Whether any positioned entity other than the scene stands on the tile. */
    private boolean isOccupied(int x, int y) {
        return entitiesToDraw.stream()
                .filter(entity -> !Objects.equals(entity, scene))
                .anyMatch(entity -> entity.getX() == x && entity.getY() == y);
    }

    private class MoveAction extends AbstractAction {
        private final Consumer<WorldScene> move;

        MoveAction(Consumer<WorldScene> move) {
            this.move = move;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (worldPause.isPaused()) {
                return;
            }
            int beforeX = player.getX();
            int beforeY = player.getY();
            move.accept(scene);
            int afterX = player.getX();
            int afterY = player.getY();
            if (beforeX != afterX || beforeY != afterY) {
                eventLog.recordEvent(GameEvent.playerMoved(afterX, afterY));
                turnScheduler.advance();
            }
            notifyPlayerUpdated();
        }
    }

    private class ToggleInventoryAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            for (GameListener l : listeners) {
                l.toggleInventory();
            }
        }
    }

    private class ToggleCodexAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            for (GameListener l : listeners) {
                l.toggleCodex();
            }
        }
    }

    private class ToggleDevConsoleAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            for (GameListener l : listeners) {
                l.toggleDevConsole();
            }
        }
    }

    private class TogglePauseAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            for (GameListener l : listeners) {
                l.togglePause();
            }
        }
    }
}
