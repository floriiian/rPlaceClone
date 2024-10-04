package org.florian.rplace.session;

import org.florian.rplace.canvas.CanvasPixel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CanvasSession implements Serializable {

    public String canvasCode;
    private final List<String> participants = new ArrayList<>();
    public CanvasPixel[][] canvasData = new CanvasPixel[1000][1000];

    public CanvasSession(String sessionCode, String creatorID){
        this.canvasCode = sessionCode;
        this.participants.add(creatorID);
    }


    public void addPixelToCanvas(int x, int y, String color, String participantID){

        canvasData[x][y] = new CanvasPixel(participantID, x , y, color);
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
