package org.florian.rplace;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;

import io.javalin.websocket.WsMessageContext;
import org.apache.commons.text.RandomStringGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.florian.rplace.canvas.CanvasPixel;
import org.florian.rplace.json.*;
import org.florian.rplace.session.CanvasSession;

import java.util.*;

public class Main {

    static Logger LOGGER = LogManager.getLogger();
    static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Set<WsContext> USERS = new HashSet<>();
    private static final List<CanvasSession> ACTIVE_CANVAS_SESSIONS = new ArrayList<>();

    public static void main() {

        Javalin app = Javalin.create().start(8888);

        // WebSocket endpoints
        app.ws("/canvas", ws -> {
            ws.onConnect(ctx -> {
                LOGGER.debug("User: {} connected.", ctx.sessionId());
                USERS.add(ctx);
            });

            ws.onMessage(ctx -> {

                String requestedData = ctx.message();
                JsonNode jsonData = OBJECT_MAPPER.readTree(requestedData);

                if(jsonData.isEmpty()){
                    return;
                }

                String requestType = jsonData.get("requestType").asText();
                CanvasSession session;

                switch(requestType) {

                    case "canvas":

                        CanvasRequest canvasRequest = OBJECT_MAPPER.treeToValue(jsonData, CanvasRequest.class);
                        session = getCanvasSessionByCode(canvasRequest.canvasCode());

                        if(session != null) {
                            try {
                                session.addParticipant(ctx.sessionId());

                                ArrayList<CanvasPixel> canvasPixels = new ArrayList<>();

                                for(CanvasPixel[] a : session.canvasData){
                                    for(CanvasPixel b : a){
                                        if(b != null){
                                            canvasPixels.add(b);
                                        }
                                    }
                                }

                                String canvasContent = OBJECT_MAPPER.writeValueAsString(canvasPixels);

                                ctx.send(OBJECT_MAPPER.writeValueAsString(
                                        new CanvasResponse("canvasResponse", canvasContent))
                                );
                                LOGGER.debug("Loaded Canvas for: {}", ctx.sessionId());
                            }
                            catch (JsonProcessingException e) {
                                LOGGER.debug("Canvas request failed", e);
                                ctx.send(OBJECT_MAPPER.writeValueAsString(false));
                            }
                        }
                        else{
                            LOGGER.debug("Canvas request failed");
                            ctx.send(OBJECT_MAPPER.writeValueAsString(false));
                        }
                        break;

                    case "draw":
                        DrawRequest drawRequest = OBJECT_MAPPER.treeToValue(jsonData, DrawRequest.class);

                        session = getCanvasSessionByID(ctx.sessionId());

                        if(session != null) {
                            int x = drawRequest.x();
                            int y = drawRequest.y();
                            String color = drawRequest.color();

                            if(color == null) {
                                cancelDrawResponse(ctx);
                                return;
                            }
                            try{
                                session.addPixelToCanvas(
                                        x,
                                        y,
                                        color,
                                        ctx.sessionId()
                                );
                                ctx.send(OBJECT_MAPPER.writeValueAsString(
                                        new DrawResponse("drawResponse", true))
                                );
                                for(WsContext user :  USERS){
                                    user.send(
                                            OBJECT_MAPPER.writeValueAsString(
                                                    new DrawUpdate("canvasUpdate", x, y, color)
                                        )
                                    );
                                }
                                LOGGER.debug("Successfully drawn");
                            }
                            catch (Error e){
                                LOGGER.debug(e);
                                cancelDrawResponse(ctx);
                                return;
                            }
                        }
                        else {
                            LOGGER.debug("Draw request failed");
                            cancelDrawResponse(ctx);
                        }
                        break;

                    case "session":
                        ctx.send(OBJECT_MAPPER.writeValueAsString(new SessionResponse(
                                "sessionResponse",
                                generateCanvasSession(ctx.sessionId())))
                        );
                        break;
                }
            });

            ws.onClose(ctx -> {
                for(CanvasSession session : ACTIVE_CANVAS_SESSIONS) {

                    String participantID = ctx.sessionId();
                    List<String> participants = session.getSessionParticipants();

                    if(participants.contains(participantID)) {
                        session.removeParticipant(participantID);
                        LOGGER.debug("Removed: {} from session.", participantID);
                    }
                    else if(session.ownerID.equals(participantID)) {
                        // TODO: Send popup to announce session termination
                        // ctx.send(OBJECT_MAPPER.writeValueAsString("SESSION_CLOSED"));
                        terminateCanvasSession(session);
                    }
                }
                USERS.remove(ctx);
            });

            ws.onError(ctx -> LOGGER.debug("An error occurred: {}", ctx.error()));
        });
        // app.get("/", ctx -> ctx.result("WebSocket server is running on ws://localhost:4124141221124/websocket"));

    }

    private static CanvasSession getCanvasSessionByID(String participantID) {
        for(CanvasSession session : ACTIVE_CANVAS_SESSIONS){
            for( String participant :session.getSessionParticipants()){
                if(participant.equals(participantID)){
                    return session;
                }
            }
        }
        return null;
    }

    private static CanvasSession getCanvasSessionByCode(String canvasCode) {
        for(CanvasSession session : ACTIVE_CANVAS_SESSIONS){
            if(session.canvasCode.equals(canvasCode)){
                return session;
            }
        }
        return null;
    }

    public static void terminateCanvasSession(CanvasSession session){
        LOGGER.debug("Removing session: {}", session.canvasCode);
        ACTIVE_CANVAS_SESSIONS.remove(session);
    }



    private static String generateCanvasSession(String ownerID){

        boolean isUniqueCanvasCode = false;
        String canvasCode = "";
        
        while(!isUniqueCanvasCode){
            canvasCode = generateCanvasCode();
            isUniqueCanvasCode = true;

            for (CanvasSession session : ACTIVE_CANVAS_SESSIONS) {
                if (session.canvasCode.equals(canvasCode)) {
                    isUniqueCanvasCode = false;
                    break;
                }
            }
        }

        ACTIVE_CANVAS_SESSIONS.add(new CanvasSession(canvasCode, ownerID ));
        LOGGER.debug("Added new canvas session: {}", canvasCode);

        return canvasCode;

    }

    private static String generateCanvasCode(){
        RandomStringGenerator generator = new RandomStringGenerator.Builder()
                .withinRange('A', 'Z').get();
        return generator.generate(8);
    }

    private static void cancelDrawResponse(WsMessageContext ctx) throws JsonProcessingException {
        ctx.send(OBJECT_MAPPER.writeValueAsString(
                new DrawResponse("drawResponse", false))
        );
    }
}
