package org.florian.rplace.canvas;

public class CanvasPixel {

    private String owner;
    private int[] position;
    private String color;

    public CanvasPixel(String owner, int[] position, String color) {
        this.owner = owner;
        this.position = position;
        this.color = color;
    }

    public String getOwner() {
        return owner;
    }

    public String getColor() {
        return color;
    }

    public int[] getPosition() {
        return position;
    }

    public void updateCanvasPixel(String owner, int[] position, String color){
        this.owner = owner;
        this.position = position;
        this.color = color;
    }
}
