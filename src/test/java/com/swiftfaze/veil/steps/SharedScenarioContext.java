package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.render.Camera;
import com.swiftfaze.veil.sandbox.DevConsoleModel;
import com.swiftfaze.veil.sandbox.DevConsolePanel;
import com.swiftfaze.veil.sandbox.KitchenSinkPreviewPanel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Shared state across step definition classes within a single scenario.
 * Uses ThreadLocal to store scenario-scoped state without requiring dependency injection.
 */
public final class SharedScenarioContext {

    private static final ThreadLocal<Camera> CAMERA = new ThreadLocal<>();
    private static final ThreadLocal<DevConsoleModel> DEV_CONSOLE_MODEL = new ThreadLocal<>();
    private static final ThreadLocal<DevConsolePanel> DEV_CONSOLE_PANEL = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, String>> QUEST_LOG_SNAPSHOT = new ThreadLocal<>();
    private static final ThreadLocal<DevConsoleSteps> DEV_CONSOLE_STEPS_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<UiComponentFrameworkSteps> UI_STEPS_HOLDER =
            ThreadLocal.withInitial(UiComponentFrameworkSteps::new); // lazily built: Cucumber only instantiates step classes whose steps run
    private static final ThreadLocal<List<Consumer<UiComponentFrameworkSteps>>> UI_STEPS_LISTENERS =
            ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<KitchenSinkPreviewPanel> PREVIEW_PANEL_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<GamePanelFixture> GAME_FIXTURE_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<GamePanelFixture> TURN_GAME_HOLDER = new ThreadLocal<>();

    private SharedScenarioContext() {
    }

    public static Camera getCamera() {
        return CAMERA.get();
    }

    public static void setCamera(Camera camera) {
        CAMERA.set(camera);
    }

    public static DevConsoleModel getDevConsoleModel() {
        return DEV_CONSOLE_MODEL.get();
    }

    public static void setDevConsoleModel(DevConsoleModel model) {
        DEV_CONSOLE_MODEL.set(model);
    }

    public static DevConsolePanel getDevConsolePanel() {
        return DEV_CONSOLE_PANEL.get();
    }

    public static void setDevConsolePanel(DevConsolePanel panel) {
        DEV_CONSOLE_PANEL.set(panel);
    }

    public static Map<String, String> getQuestLogSnapshot() {
        return QUEST_LOG_SNAPSHOT.get();
    }

    public static void setQuestLogSnapshot(Map<String, String> snapshot) {
        QUEST_LOG_SNAPSHOT.set(snapshot);
    }

    public static DevConsoleSteps getDevConsoleSteps() {
        return DEV_CONSOLE_STEPS_HOLDER.get();
    }

    public static void setDevConsoleSteps(DevConsoleSteps steps) {
        DEV_CONSOLE_STEPS_HOLDER.set(steps);
    }

    public static UiComponentFrameworkSteps getUiSteps() {
        return UI_STEPS_HOLDER.get();
    }

    public static void setUiSteps(UiComponentFrameworkSteps steps) {
        UI_STEPS_HOLDER.set(steps);
        UI_STEPS_LISTENERS.get().forEach(listener -> listener.accept(steps));
    }

    /**
     * Applies {@code setup} to the current UI steps instance and to every instance Cucumber builds
     * later in this scenario. Cucumber creates its own {@code UiComponentFrameworkSteps} lazily on the
     * first of its steps, replacing any instance another glue class obtained earlier.
     */
    public static void applyToUiSteps(Consumer<UiComponentFrameworkSteps> setup) {
        UI_STEPS_LISTENERS.get().add(setup);
        setup.accept(getUiSteps());
    }

    public static GamePanelFixture getTurnGame() {
        return TURN_GAME_HOLDER.get();
    }

    public static void setTurnGame(GamePanelFixture game) {
        TURN_GAME_HOLDER.set(game);
    }

    public static KitchenSinkPreviewPanel getKitchenSinkPreviewPanel() {
        return PREVIEW_PANEL_HOLDER.get();
    }

    public static void setKitchenSinkPreviewPanel(KitchenSinkPreviewPanel panel) {
        PREVIEW_PANEL_HOLDER.set(panel);
    }

    /** The game panel the current scenario drives, or null when it drives none. */
    public static GamePanelFixture getGameFixture() {
        return GAME_FIXTURE_HOLDER.get();
    }

    static void setGameFixture(GamePanelFixture fixture) {
        GAME_FIXTURE_HOLDER.set(fixture);
    }

    public static void cleanup() {
        CAMERA.remove();
        DEV_CONSOLE_MODEL.remove();
        DEV_CONSOLE_PANEL.remove();
        QUEST_LOG_SNAPSHOT.remove();
        DEV_CONSOLE_STEPS_HOLDER.remove();
        UI_STEPS_HOLDER.remove();
        UI_STEPS_LISTENERS.remove();
        PREVIEW_PANEL_HOLDER.remove();
        GAME_FIXTURE_HOLDER.remove();
        TURN_GAME_HOLDER.remove();
    }
}
