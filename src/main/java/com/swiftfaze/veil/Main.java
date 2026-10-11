package com.swiftfaze.veil;

import com.swiftfaze.veil.config.SettingsConfig;
import com.swiftfaze.veil.config.SettingsStore;
import com.swiftfaze.veil.game.GamePanel;
import com.swiftfaze.veil.game.event.GameEventLog;
import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;
import com.swiftfaze.veil.mods.WidgetColorTheme;
import com.swiftfaze.veil.ui.ChestPanel;
import com.swiftfaze.veil.ui.ChestPanelListener;
import com.swiftfaze.veil.ui.DialoguePanel;
import com.swiftfaze.veil.ui.DialoguePanelListener;
import com.swiftfaze.veil.ui.CodexPanel;
import com.swiftfaze.veil.ui.GameWindow;
import com.swiftfaze.veil.ui.InventoryPanel;
import com.swiftfaze.veil.ui.PauseMenuPopup;
import com.swiftfaze.veil.ui.PauseToggleListener;
import com.swiftfaze.veil.ui.PopupPanels;
import com.swiftfaze.veil.ui.PopupToggleListener;
import com.swiftfaze.veil.ui.ScreenNavigator;
import com.swiftfaze.veil.ui.SettingsKeybindsPanel;
import com.swiftfaze.veil.ui.SettingsKeybindsWindow;
import com.swiftfaze.veil.ui.SettingsScreenPanel;
import com.swiftfaze.veil.ui.SettingsWindow;
import com.swiftfaze.veil.ui.TitleScreenPanel;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import com.swiftfaze.veil.ui.widget.FocusManager;
import com.swiftfaze.veil.ui.widget.WidgetTheme;
import com.swiftfaze.veil.sandbox.DevConsolePanel;
import com.swiftfaze.veil.sandbox.DevConsoleModel;
import com.swiftfaze.veil.sandbox.DevConsoleProvider;
import com.swiftfaze.veil.sandbox.WorldRegenerator;
import com.swiftfaze.veil.sandbox.PlayerSandboxProvider;
import com.swiftfaze.veil.sandbox.ClassSandboxProvider;
import com.swiftfaze.veil.sandbox.ItemSandboxProvider;
import com.swiftfaze.veil.sandbox.QuestSandboxProvider;
import com.swiftfaze.veil.sandbox.TileSandboxProvider;
import com.swiftfaze.veil.sandbox.KitchenSinkProvider;
import com.swiftfaze.veil.game.GameListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Main {

    private Main() {
    }
    /**
     * The card deck the screens live in: what to do with it (add a screen, register a
     * card for lookup, navigate, read the registry) without handing out the Swing
     * container or the mutable registry itself.
     */
    private static final class ScreenDeck {
        private final BiConsumer<Component, String> addScreen;
        private final Consumer<JFrame> addTo;
        private final BiConsumer<String, JComponent> register;
        private final Map<String, JComponent> cardsView;
        private final Consumer<String> navigateTo;

        ScreenDeck(JPanel cardPanel, Map<String, JComponent> cards, Consumer<String> navigateTo) {
            this.addScreen = cardPanel::add;
            this.addTo = frame -> frame.add(cardPanel, BorderLayout.CENTER);
            this.register = cards::put;
            this.cardsView = Collections.unmodifiableMap(cards);
            this.navigateTo = navigateTo;
        }

        void addScreen(Component screen, String name) {
            addScreen.accept(screen, name);
        }

        void addTo(JFrame frame) {
            addTo.accept(frame);
        }

        void register(String name, JComponent card) {
            register.accept(name, card);
        }

        JComponent card(String name) {
            return cardsView.get(name);
        }

        Map<String, JComponent> cardsView() {
            return cardsView;
        }

        void navigateTo(String name) {
            navigateTo.accept(name);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final List<ControlsHintBarWidget.Hint> GAME_HINTS = List.of(
            new ControlsHintBarWidget.Hint("i", "Inventory"),
            new ControlsHintBarWidget.Hint("x", "Codex"),
            new ControlsHintBarWidget.Hint("e", "Interact"));
    /** The static controls hints shown on the world screen. */
    public static List<ControlsHintBarWidget.Hint> gameHints() {
        return GAME_HINTS;
    }

    private static final boolean DEV_CONSOLE_ENABLED = Boolean.getBoolean("veil.devConsole");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::loadGame);
    }

    private static void loadGame() {
        JFrame frame = new JFrame("Veil");
        CardLayout cardLayout = new CardLayout();
        JPanel cardPanel = new JPanel(cardLayout);
        loadAndApplyDefaultTheme();
        Map<String, JComponent> cards = new HashMap<>();
        ControlsHintBarWidget hintBar = new ControlsHintBarWidget();
        GameEventLog eventLog = GameEventLog.fromSystemProperties();

        hintBar.setVersionText(new AppVersion().getDisplayVersion());

        GamePanel gamePanel = buildGameCard(cardPanel, cards, hintBar, eventLog);
        ScreenNavigator navigator = new ScreenNavigator(cardLayout, cardPanel, cards, eventLog);
        ScreenDeck deck = new ScreenDeck(cardPanel, cards, navigator::navigateTo);
        buildUiScreens(deck, gamePanel, hintBar, eventLog);
        wirePauseMenuNavigation(deck, gamePanel);
        wireDevConsole(gamePanel);
        configureAndShowFrame(frame, deck, hintBar, navigator);
    }

    private static GamePanel buildGameCard(JPanel cardPanel, Map<String, JComponent> cards, ControlsHintBarWidget hintBar, GameEventLog eventLog) {
        GamePanel gamePanel = new GamePanel(eventLog);
        gamePanel.spawnNpcs(ModLoader.load(Paths.get("mods")));
        InventoryPanel inventoryPanel = new InventoryPanel(hintBar);
        CodexPanel codexPanel = new CodexPanel(hintBar);
        PauseMenuPopup pauseMenuPopup = new PauseMenuPopup();
        cards.put("pause", pauseMenuPopup);
        ChestPanel chestPanel = new ChestPanel(hintBar::setHints);
        FocusManager focusManager = wirePopups(gamePanel, new PopupPanels(inventoryPanel, codexPanel, pauseMenuPopup), hintBar, eventLog);
        wireChestPanel(gamePanel, chestPanel, focusManager, hintBar);
        DialoguePanel dialoguePanel = new DialoguePanel(hintBar::setHints);
        wireDialoguePanel(gamePanel, dialoguePanel, focusManager, hintBar);

        JLayeredPane gameContentArea = GameWindow.buildContentArea(gamePanel);
        gameContentArea.add(inventoryPanel, JLayeredPane.POPUP_LAYER);
        gameContentArea.add(codexPanel, JLayeredPane.POPUP_LAYER);
        gameContentArea.add(pauseMenuPopup, JLayeredPane.POPUP_LAYER);
        gameContentArea.add(chestPanel, JLayeredPane.POPUP_LAYER);
        gameContentArea.add(dialoguePanel, JLayeredPane.POPUP_LAYER);
        gameContentArea.add(inventoryPanel.getDropConfirmationPopup(), JLayeredPane.DRAG_LAYER);

        JPanel gameCardPanel = new JPanel(new BorderLayout());
        gameCardPanel.add(gameContentArea, BorderLayout.CENTER);
        cardPanel.add(gameCardPanel, "game");
        return gamePanel;
    }

    // Minimal stopgap wiring so the I/X toggles keep working with EastPanel gone — no
    // sidebar, no player-info display, just enough plumbing for the two popups to open/close/exclude
    // each other and hand focus back to the game on dismiss, same as EastPanel used to.
    private static FocusManager wirePopups(GamePanel gamePanel, PopupPanels popups, ControlsHintBarWidget hintBar, GameEventLog eventLog) {
        ModRegistry mods = ModLoader.load(Paths.get("mods"));
        FocusManager focusManager = new FocusManager();
        InventoryPanel inventoryPanel = popups.inventory();
        CodexPanel codexPanel = popups.codex();
        PauseMenuPopup pauseMenuPopup = popups.pause();

        inventoryPanel.setFocusManager(focusManager);
        inventoryPanel.showItems(mods.getAllItems());
        inventoryPanel.setOnDismiss(() -> restoreGameFocus(gamePanel, hintBar));

        codexPanel.setFocusManager(focusManager);
        codexPanel.showItems(mods.getAllItems());
        codexPanel.showTiles(mods.getAllTiles());
        codexPanel.showClasses(mods.getAllPlayerClasses());
        codexPanel.setOnDismiss(() -> restoreGameFocus(gamePanel, hintBar));

        pauseMenuPopup.setFocusManager(focusManager);
        pauseMenuPopup.setOnDismiss(() -> restoreGameFocus(gamePanel, hintBar));

        gamePanel.addGameListener(new PopupToggleListener(inventoryPanel, codexPanel, gamePanel.getWorldPause(), eventLog));
        gamePanel.addGameListener(new PauseToggleListener(gamePanel.getWorldPause(), pauseMenuPopup));
        return focusManager;
    }

    private static void wireChestPanel(GamePanel gamePanel, ChestPanel chestPanel, FocusManager focusManager, ControlsHintBarWidget hintBar) {
        chestPanel.setFocusManager(focusManager);
        chestPanel.setOnDismiss(() -> restoreGameFocus(gamePanel, hintBar));
        gamePanel.addGameListener(new ChestPanelListener(chestPanel, gamePanel.getWorldPause()));
    }

    private static void wireDialoguePanel(GamePanel gamePanel, DialoguePanel dialoguePanel, FocusManager focusManager, ControlsHintBarWidget hintBar) {
        dialoguePanel.setFocusManager(focusManager);
        dialoguePanel.setOnDismiss(() -> restoreGameFocus(gamePanel, hintBar));
        gamePanel.addGameListener(new DialoguePanelListener(dialoguePanel, gamePanel.getWorldPause()));
    }

    private static void restoreGameFocus(GamePanel gamePanel, ControlsHintBarWidget hintBar) {
        gamePanel.requestFocusInWindow();
        hintBar.setHints(GAME_HINTS);
    }

    private static void buildUiScreens(ScreenDeck deck, GamePanel gamePanel, ControlsHintBarWidget hintBar, GameEventLog eventLog) {
        TitleScreenPanel titleScreen = new TitleScreenPanel(menuItem -> {
            handleMenuSelection(menuItem, deck, gamePanel);
            if ("New".equals(menuItem)) {
                hintBar.setHints(GAME_HINTS);
            }
        }, hintBar, eventLog);
        SettingsStore settingsStore = new SettingsStore(Path.of("").toAbsolutePath());
        SettingsScreenPanel settingsScreen = new SettingsScreenPanel(
                screen -> handleSettingsBack(screen, deck), Main::openFolder, hintBar, settingsStore);
        SettingsKeybindsPanel keybindsScreen = new SettingsKeybindsPanel(
                deck::navigateTo, hintBar, settingsStore);
        deck.register("title", titleScreen);
        deck.register("settings", settingsScreen);
        deck.register("keybinds", keybindsScreen);
        deck.addScreen(titleScreen, "title");
        deck.addScreen(SettingsWindow.buildContentArea(settingsScreen), "settings");
        deck.addScreen(SettingsKeybindsWindow.buildContentArea(keybindsScreen), "keybinds");
    }

    private static void handleMenuSelection(String menuItem, ScreenDeck deck, GamePanel gamePanel) {
        if ("New".equals(menuItem)) {
            deck.navigateTo("game");
            gamePanel.requestFocusInWindow();
            gamePanel.startGameLoop();
        } else if ("Settings".equals(menuItem)) {
            ((SettingsScreenPanel) deck.card("settings")).setBackTarget("title");
            deck.navigateTo("settings");
        } else if ("Exit".equals(menuItem)) {
            System.exit(0);
        }
    }

    private static void handleSettingsBack(String screen, ScreenDeck deck) {
        if ("pause".equals(screen)) {
            deck.navigateTo("game");
            deck.card("pause").requestFocusInWindow();
        } else {
            deck.navigateTo(screen);
        }
    }

    private static void wirePauseMenuNavigation(ScreenDeck deck, GamePanel gamePanel) {
        PauseMenuPopup pauseMenuPopup = (PauseMenuPopup) deck.card("pause");
        SettingsScreenPanel settingsScreen = (SettingsScreenPanel) deck.card("settings");
        pauseMenuPopup.setOnMenuSelect(item -> {
            if (PauseMenuPopup.SETTINGS.equals(item)) {
                settingsScreen.setBackTarget("pause");
                deck.navigateTo("settings");
            } else if (PauseMenuPopup.EXIT_TO_MAIN_MENU.equals(item)) {
                gamePanel.resetState();
                gamePanel.spawnNpcs(ModLoader.load(Paths.get("mods")));
                deck.navigateTo("title");
            }
        });
    }

    private static void wireDevConsole(GamePanel gamePanel) {
        if (!DEV_CONSOLE_ENABLED) {
            return;
        }

        List<DevConsoleProvider> providers = buildDevConsoleProviders(gamePanel::getPlayer);
        DevConsoleModel model = new DevConsoleModel(providers);
        Random freshSeeds = new Random();
        WorldRegenerator regenerator = new WorldRegenerator() {
            @Override
            public long freshSeed() {
                return freshSeeds.nextLong();
            }

            @Override
            public Optional<String> regenerate(long seed) {
                gamePanel.regenerateWorld(seed);
                return Optional.empty();
            }
        };
        DevConsolePanel console = new DevConsolePanel(model, regenerator);

        JFrame consoleFrame = new JFrame("Veil - Dev Console");
        consoleFrame.add(console);
        AppIcon.applyTo(consoleFrame::setIconImage);

        consoleFrame.pack();
        consoleFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        consoleFrame.setLocationRelativeTo(null);

        gamePanel.addGameListener(new GameListener() {
            @Override
            public void toggleDevConsole() {
                consoleFrame.setVisible(!consoleFrame.isVisible());
                if (consoleFrame.isVisible()) {
                    // toFront() first: requestFocus() alone can be silently denied by the OS's
                    // focus-stealing prevention, leaving the game window (which still has real
                    // input focus from the keypress that opened this) swallowing every
                    // subsequent key press instead of the console.
                    consoleFrame.toFront();
                    consoleFrame.requestFocus();
                    console.requestFocusInWindow();
                }
            }

            @Override
            public void updatePlayer(Player player) {
            }
        });
    }

    public static List<DevConsoleProvider> buildDevConsoleProviders(Supplier<Player> playerSupplier) {
        return List.of(
            new ClassSandboxProvider(),
            new ItemSandboxProvider(),
            new QuestSandboxProvider(),
            new TileSandboxProvider(),
            new PlayerSandboxProvider(playerSupplier),
            new KitchenSinkProvider()
        );
    }

    private static void configureAndShowFrame(JFrame frame, ScreenDeck deck, ControlsHintBarWidget hintBar,
                                              ScreenNavigator navigator) {
        // Border lives on the frame's content pane, not on any individual screen, so it's
        // flush against the true window edge and shows on every card (title/settings/
        // keybinds/game) uniformly rather than only around whichever panel drew its own.
        ((JComponent) frame.getContentPane()).setBorder(
                BorderFactory.createLineBorder(WidgetTheme.windowBorder(), 2));
        frame.setLayout(new BorderLayout());
        deck.addTo(frame);
        frame.add(hintBar, BorderLayout.SOUTH);
        AppIcon.applyTo(frame::setIconImage);

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        wireWindowMode(frame, deck.cardsView());
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        navigator.showInitial("title");
        // cardPanel.getComponent(0) is whichever card was added to the container FIRST
        // (the "game" card, added in buildGameCard() before buildUiScreens() adds "title") -
        // not whichever card CardLayout is currently showing. Requesting focus on that
        // hidden, non-showing component silently fails, so no component ever holds
        // keyboard focus. Look the actually-visible card up by name instead.
        deck.card("title").requestFocusInWindow();
    }

    // Must run before any screen/widget is constructed below - they read WidgetTheme's
    // statics at construction time. No settings/config system exists yet to pick a
    // non-default theme, so this always applies whichever mod owns ID "core:default".
    private static void loadAndApplyDefaultTheme() {
        ModRegistry mods = ModLoader.load(Paths.get("mods"));
        WidgetColorTheme defaultTheme = mods.getTheme("core:default");
        if (defaultTheme != null) {
            WidgetTheme.applyTheme(defaultTheme);
        }
    }

    private static void openFolder(String which) {
        try {
            Path base = Path.of("").toAbsolutePath();
            File target = "mods".equals(which) ? base.resolve("mods").toFile() : base.toFile();
            if ("mods".equals(which) && !target.exists()) {
                Files.createDirectories(target.toPath());
            }
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(target);
            }
        } catch (IOException e) {
            logger.warn("Failed to open folder: {}", which, e);
        }
    }

    private static void wireWindowMode(JFrame frame, Map<String, JComponent> cards) {
        SettingsScreenPanel settingsScreen = (SettingsScreenPanel) cards.get("settings");
        SettingsStore settingsStore = settingsScreen.getSettingsStore();
        settingsScreen.setOnWindowModeChanged(mode -> {
            applyWindowMode(frame, mode, settingsStore);
            settingsScreen.requestFocusInWindow();
        });
        registerWindowSizeShutdownHook(frame, settingsStore);
    }

    private static final int MIN_WINDOW_WIDTH = 400;
    private static final int MIN_WINDOW_HEIGHT = 300;

    private static void applyWindowMode(JFrame frame, String mode, SettingsStore settingsStore) {
        boolean fullscreen = "Fullscreen".equals(mode);
        boolean wasDisplayable = frame.isDisplayable();
        if (wasDisplayable) {
            frame.dispose(); // Swing requires this before setUndecorated on an already-shown frame
        }
        frame.setUndecorated(fullscreen);
        frame.setResizable(!fullscreen);
        if (fullscreen) {
            frame.setBounds(currentScreenBounds(frame));
        } else {
            frame.setMinimumSize(new Dimension(MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT));
            frame.pack();
            restoreSavedWindowSize(frame, settingsStore);
        }
        if (wasDisplayable) {
            frame.setVisible(true);
        }
    }

    private static void restoreSavedWindowSize(JFrame frame, SettingsStore settingsStore) {
        SettingsConfig config = settingsStore.config();
        int savedWidth = config.getWindowWidth();
        int savedHeight = config.getWindowHeight();
        if (savedWidth > 0 && savedHeight > 0) {
            frame.setSize(clampToScreenBounds(frame, savedWidth, savedHeight));
        }
    }

    private static Dimension clampToScreenBounds(JFrame frame, int width, int height) {
        Rectangle bounds = currentScreenBounds(frame);
        int clampedWidth = Math.max(MIN_WINDOW_WIDTH, Math.min(width, bounds.width));
        int clampedHeight = Math.max(MIN_WINDOW_HEIGHT, Math.min(height, bounds.height));
        return new Dimension(clampedWidth, clampedHeight);
    }

    private static Rectangle currentScreenBounds(JFrame frame) {
        GraphicsConfiguration config = frame.getGraphicsConfiguration();
        GraphicsDevice device = config != null
                ? config.getDevice()
                : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        return device.getDefaultConfiguration().getBounds();
    }

    private static void registerWindowSizeShutdownHook(JFrame frame, SettingsStore settingsStore) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!frame.isUndecorated()) {
                SettingsConfig config = settingsStore.config();
                config.setWindowWidth(frame.getWidth());
                config.setWindowHeight(frame.getHeight());
                settingsStore.persist();
            }
        }));
    }

}
