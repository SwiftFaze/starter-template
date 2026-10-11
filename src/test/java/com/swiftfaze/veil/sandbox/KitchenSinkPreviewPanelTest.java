package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.GameConst;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.widget.WidgetTheme;
import com.swiftfaze.veil.world.KitchenSinkScene;
import com.swiftfaze.veil.world.Tile;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitchenSinkPreviewPanelTest {

    private static final int TILE = GameConst.TILE_WIDTH;
    // Glyphs are drawn in a colour no tint or marker uses, so pixel checks can tell them apart.
    private static final Tile FLOOR = new Tile("test:floor", ' ', new Color(1, 2, 3), true);
    private static final Tile WALL = new Tile("test:wall", ' ', new Color(4, 5, 6), false);

    private static KitchenSinkPreviewPanel panelOver(List<Tile> tiles) {
        return new KitchenSinkPreviewPanel(KitchenSinkScene.holding(tiles));
    }

    private static List<Tile> twelveFloors() {
        return new ArrayList<>(Collections.nCopies(12, FLOOR));
    }

    private static BufferedImage paint(KitchenSinkPreviewPanel target, int width, int height) {
        target.setSize(width, height);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        target.paint(g);
        g.dispose();
        return image;
    }

    private static int tintedBackground(Color tint) {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(WidgetTheme.background());
        g.fillRect(0, 0, 1, 1);
        g.setColor(tint);
        g.fillRect(0, 0, 1, 1);
        g.dispose();
        return image.getRGB(0, 0);
    }

    private static Point markerCentroid(BufferedImage image) {
        long sumX = 0;
        long sumY = 0;
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) == WidgetTheme.PREVIEW_MARKER.getRGB()) {
                    sumX += x;
                    sumY += y;
                    count++;
                }
            }
        }
        assertTrue(count > 0, "No marker pixels drawn");
        return new Point((int) (sumX / count), (int) (sumY / count));
    }

    private static void press(KitchenSinkPreviewPanel target, KeyStroke key) {
        Object actionName = target.getInputMap(JComponent.WHEN_FOCUSED).get(key);
        target.getActionMap().get(actionName).actionPerformed(new ActionEvent(target, 0, ""));
    }

    @Test
    void thePanelCanTakeKeyboardFocus() {
        assertTrue(panelOver(twelveFloors()).isFocusable());
    }

    @Test
    void theWKeyTogglesTheOverlayThroughTheFocusedInputMap() {
        KitchenSinkPreviewPanel panel = panelOver(twelveFloors());

        press(panel, Keybindings.TOGGLE_WALKABILITY);

        assertEquals(tintedBackground(WidgetTheme.WALKABLE_TINT), paint(panel, TILE, TILE).getRGB(1, 1));
    }

    @Test
    void theArrowKeyMovesTheMarkerThroughTheFocusedInputMap() {
        KitchenSinkPreviewPanel panel = panelOver(twelveFloors());

        press(panel, Keybindings.MOVE_RIGHT_ARROW);

        assertEquals(1, markerCentroid(paint(panel, 10 * TILE, 3 * TILE)).x / TILE);
    }

    @Test
    void aSceneWithTilesShowsThePreview() {
        assertTrue(panelOver(twelveFloors()).isPreviewShown());
    }

    @Test
    void theEmptyStateLabelUsesThemeTextColourAtBodySize() {
        KitchenSinkPreviewPanel panel = panelOver(List.of());

        JLabel label = (JLabel) panel.getComponent(0);

        assertEquals(WidgetTheme.normalText(), label.getForeground());
        assertEquals(16, label.getFont().getSize());
    }

    @Test
    void walkableAndUnwalkableCellsGetTheirOwnTint() {
        KitchenSinkPreviewPanel panel = panelOver(List.of(FLOOR, WALL));
        press(panel, Keybindings.TOGGLE_WALKABILITY);

        BufferedImage image = paint(panel, 2 * TILE, TILE);

        assertEquals(tintedBackground(WidgetTheme.WALKABLE_TINT), image.getRGB(1, 1));
        assertEquals(tintedBackground(WidgetTheme.UNWALKABLE_TINT), image.getRGB(TILE + 1, 1));
    }

    @Test
    void theOverlayStopsAtTheSceneEdge() {
        KitchenSinkPreviewPanel panel = panelOver(twelveFloors());
        press(panel, Keybindings.TOGGLE_WALKABILITY);

        BufferedImage image = paint(panel, 12 * TILE, 4 * TILE);

        assertEquals(WidgetTheme.background().getRGB(), image.getRGB(10 * TILE + 1, 1));
        assertEquals(WidgetTheme.background().getRGB(), image.getRGB(1, 2 * TILE + 1));
    }

    @Test
    void theMarkerIsDrawnInsideItsOwnCell() {
        KitchenSinkPreviewPanel panel = panelOver(twelveFloors());
        press(panel, Keybindings.MOVE_RIGHT_ARROW);
        press(panel, Keybindings.MOVE_DOWN_ARROW);

        Point centroid = markerCentroid(paint(panel, 10 * TILE, 3 * TILE));

        assertEquals(1, centroid.x / TILE, "Marker not in column 1");
        assertEquals(1, centroid.y / TILE, "Marker not in row 1");
    }

    @Test
    void anEmptySceneDrawsNoPreview() {
        assertFalse(panelOver(List.of()).isPreviewShown());
    }
}
