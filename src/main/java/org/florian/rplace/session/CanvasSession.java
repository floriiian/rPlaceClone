package org.florian.rplace.session;

import org.florian.rplace.canvas.CanvasPixel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CanvasSession {

    public String canvasCode;
    public String ownerID;
    private List<String> participants = new ArrayList<>();
    public List<CanvasPixel> canvasData = new ArrayList<>();

    public CanvasSession(String sessionCode, String ownerID){
        this.canvasCode = sessionCode;
        this.ownerID = ownerID;
        this.participants.add(ownerID);
    }


    public void addPixelToCanvas(int[] pixelPosition, String color, String participantID){

        boolean pixelReplaced = false;
        Iterator<CanvasPixel> iterator = canvasData.iterator();

        // Using iterators makes it possible to delete values while iterating,
        // otherwise ConcurrentModificationException occurs.

        while (iterator.hasNext()) {
            CanvasPixel data = iterator.next();
            int[] currentPixelPosition = data.getPosition();
            if (currentPixelPosition[0] == pixelPosition[0] && currentPixelPosition[1] == pixelPosition[1]) {
                iterator.remove();
                pixelReplaced = true;
                break;
            }
        }
        if(!pixelReplaced){
            canvasData.add(new CanvasPixel(participantID, pixelPosition, color));
        }
    }

    public List<String> getSessionParticipants(){
        return this.participants;
    }

    public void addParticipant(String participantID){
        this.participants.add(participantID);
    }

    public void removeParticipant(String participantID){
        this.participants.remove(participantID);
    }
}
