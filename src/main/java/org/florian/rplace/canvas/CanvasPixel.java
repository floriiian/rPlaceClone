package org.florian.rplace.canvas;

import java.io.Serial;
import java.io.Serializable;

public class CanvasPixel implements Serializable {

    private final String owner;
    private final String color;
    private final PixelPosition position;


    public CanvasPixel(String owner, int x, int y, String color) {
        this.owner = owner;
        this.position = new PixelPosition(x, y);
        this.color = color;
    }

    public String getOwner() {
        return owner;
    }

    public String getColor() {
        return color;
    }

    public PixelPosition getPosition() {
        return position;
    }
}
