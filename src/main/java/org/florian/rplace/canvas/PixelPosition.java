package org.florian.rplace.canvas;

public record PixelPosition(int x, int y) {

    public int getX(){
        return x;
    }
    public int getY(){
        return y;
    }
}
