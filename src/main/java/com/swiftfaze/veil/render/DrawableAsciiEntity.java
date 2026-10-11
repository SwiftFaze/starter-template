package com.swiftfaze.veil.render;

import java.awt.Color;
import java.awt.Graphics2D;

public interface DrawableAsciiEntity extends Positionable {
    char getSymbol();
    Color getColor();
    void render(Graphics2D g2d, int tileWidth, int tileHeight, Camera camera);
}
