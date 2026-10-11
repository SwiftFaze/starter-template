package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.GameConst;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.render.Camera;
import com.swiftfaze.veil.ui.widget.WidgetTheme;
import com.swiftfaze.veil.world.WorldScene;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;

/**
 * Live preview panel for the kitchen-sink scene with a marker, movement
 * controls, and a toggleable walkability overlay. Each time opened, the
 * panel starts fresh (marker at start position, overlay off).
 */
public final class KitchenSinkPreviewPanel extends JPanel {
    public static final String EMPTY_MESSAGE = "No tiles registered";
    private static final int VIEWPORT_TILES_WIDE = 20;
    private static final int VIEWPORT_TILES_HIGH = 15;
    private static final int EMPTY_LABEL_FONT_SIZE = 16;
    private static final int MARKER_FONT_SIZE = 18;
    private static final char MARKER_GLYPH = '◼';

    private final WorldScene scene;
    private final KitchenSinkModel model;
    private final Camera camera;
    private final boolean hasAnyTiles;

    public KitchenSinkPreviewPanel(WorldScene scene) {
        this.scene = scene;
        this.model = new KitchenSinkModel(scene);
        this.camera = new Camera(VIEWPORT_TILES_WIDE, VIEWPORT_TILES_HIGH);
        this.hasAnyTiles = model.hasTiles();
        setFocusable(true);
        setBackground(WidgetTheme.background());
        if (!hasAnyTiles) {
            add(buildEmptyLabel());
        }
        bindKeys();
    }

    private void bindKeys() {
        bindMove(Keybindings.ACTION_MOVE_UP, 0, -1, Keybindings.MOVE_UP_Z, Keybindings.MOVE_UP_ARROW);
        bindMove(Keybindings.ACTION_MOVE_DOWN, 0, 1, Keybindings.MOVE_DOWN_S, Keybindings.MOVE_DOWN_ARROW);
        bindMove(Keybindings.ACTION_MOVE_LEFT, -1, 0, Keybindings.MOVE_LEFT_Q, Keybindings.MOVE_LEFT_ARROW);
        bindMove(Keybindings.ACTION_MOVE_RIGHT, 1, 0, Keybindings.MOVE_RIGHT_D, Keybindings.MOVE_RIGHT_ARROW);
        bindAction(Keybindings.ACTION_TOGGLE_WALKABILITY, model::toggleOverlay, Keybindings.TOGGLE_WALKABILITY);
    }

    private void bindMove(String actionName, int dx, int dy, KeyStroke... keys) {
        bindAction(actionName, () -> model.moveMarker(dx, dy), keys);
    }

    private void bindAction(String actionName, Runnable effect, KeyStroke... keys) {
        for (KeyStroke key : keys) {
            getInputMap(WHEN_FOCUSED).put(key, actionName);
        }
        getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                effect.run();
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!hasAnyTiles) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        // Tint first so it sits behind the glyphs, leaving every glyph fully drawn.
        if (model.isOverlayOn()) {
            drawWalkabilityOverlay(g2d);
        }
        drawScene(g2d);
        drawMarker(g2d);
    }

    public boolean isPreviewShown() {
        return hasAnyTiles;
    }

    private static JLabel buildEmptyLabel() {
        JLabel label = new JLabel(EMPTY_MESSAGE);
        label.setForeground(WidgetTheme.normalText());
        label.setFont(new Font(Font.MONOSPACED, Font.PLAIN, EMPTY_LABEL_FONT_SIZE));
        return label;
    }

    private void drawScene(Graphics2D g2d) {
        scene.renderWorld(g2d, GameConst.TILE_WIDTH, GameConst.TILE_HEIGHT, camera);
    }

    private void drawWalkabilityOverlay(Graphics2D g2d) {
        for (int x = 0; x < scene.getWidth(); x++) {
            for (int y = 0; y < scene.getHeight(); y++) {
                drawOverlayCell(g2d, x, y);
            }
        }
    }

    private void drawOverlayCell(Graphics2D g2d, int x, int y) {
        int tileW = GameConst.TILE_WIDTH;
        int tileH = GameConst.TILE_HEIGHT;
        int screenX = (x - camera.getX()) * tileW;
        int screenY = (y - camera.getY()) * tileH;

        g2d.setColor(scene.isWalkable(x, y) ? WidgetTheme.WALKABLE_TINT : WidgetTheme.UNWALKABLE_TINT);
        g2d.fillRect(screenX, screenY, tileW, tileH);
    }

    private void drawMarker(Graphics2D g2d) {
        int tileW = GameConst.TILE_WIDTH;
        int tileH = GameConst.TILE_HEIGHT;
        int screenX = (model.getMarkerX() - camera.getX()) * tileW;
        int screenY = (model.getMarkerY() - camera.getY()) * tileH + tileH;

        g2d.setFont(new Font(Font.MONOSPACED, Font.PLAIN, MARKER_FONT_SIZE));
        g2d.setColor(WidgetTheme.PREVIEW_MARKER);
        g2d.drawString(String.valueOf(MARKER_GLYPH), screenX, screenY);
    }

    public KitchenSinkModel getModel() {
        return model;
    }
}
