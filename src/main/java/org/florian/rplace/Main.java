package org.florian.rplace;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.websocket.WsContext;

import io.javalin.websocket.WsMessageContext;
import org.apache.commons.text.RandomStringGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.florian.rplace.json.*;
import org.florian.rplace.session.CanvasSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    static Logger LOGGER = LogManager.getLogger();
    static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static Set<WsContext> USERS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static List<CanvasSession> ACTIVE_CANVAS_SESSIONS = new ArrayList<>();

    // Ensures that adding, removing, or iterating over the user's
    // set happens safely when accessed by multiple threads. (Normal HashSet could lead to corruption)
    // A set only allows one unique entry, we don't want the same session twice.

    public static void main(String[] args) {

        Javalin app = Javalin.create().start(8888);

        // WebSocket endpoints
        app.ws("/websocket", ws -> {
            ws.onConnect(ctx -> {
                // TODO: In the future check the url params and create a new session
                //       if that url doesn't exist already, if it does connect to it

                System.out.println("User: " + ctx.sessionId() + " connected.");
                USERS.add(ctx); // Add the user to the set
            });

            ws.onMessage(ctx -> {

                String requestedData = ctx.message();
                JsonNode jsonData = OBJECT_MAPPER.readTree(requestedData);
                String requestType = jsonData.get("requestType").asText();

                CanvasSession session;

                switch(requestType) {

                    case "canvas":
                        session = getCanvasSession(ctx.sessionId());
                        if(session != null) {
                            try {
                                String canvasContent = OBJECT_MAPPER.writeValueAsString(session.canvasData);
                                ctx.send(OBJECT_MAPPER.writeValueAsString(canvasContent));
                                LOGGER.debug(canvasContent);
                            }
                            catch (JsonProcessingException e) {
                                LOGGER.debug("Canvas request failed", e);
                                ctx.send(OBJECT_MAPPER.writeValueAsString(false));
                            }
                        }
                        break;

                    case "draw":
                        DrawRequest drawRequest = OBJECT_MAPPER.treeToValue(jsonData, DrawRequest.class);

                        session = getCanvasSession(ctx.sessionId());

                        if(session != null) {
                            int[] position = drawRequest.position();
                            String color = drawRequest.color();

                            if(position == null || color == null) {
                                cancelDrawResponse(ctx);
                                return;
                            }
                            try{
                                session.addPixelToCanvas(
                                        position,
                                        color,
                                        ctx.sessionId()
                                );
                                ctx.send(OBJECT_MAPPER.writeValueAsString(
                                        new DrawResponse("drawResponse", true))
                                );
                                for(WsContext user :  USERS){
                                    user.send(
                                            OBJECT_MAPPER.writeValueAsString(
                                                    new DrawUpdate("drawResponse", position, color)
                                        )
                                    );
                                }
                            }
                            catch (Error e){
                                LOGGER.debug(e);

                                cancelDrawResponse(ctx);
                                return;
                            }
                        }
                        else {
                            cancelDrawResponse(ctx);
                        }
                        break;

                    case "session":
                        ctx.send(OBJECT_MAPPER.writeValueAsString(new SessionResponse(
                                "SessionResponse", generateCanvasSession(ctx.sessionId())))
                        );
                        break;
                }
                System.out.println("Received message: " + ctx.message());
            });

            ws.onClose(ctx -> {
                System.out.println("User disconnected with session ID: " + ctx.sessionId());
                USERS.remove(ctx); // Remove the user from the set
            });

            ws.onError(ctx -> {
                System.out.println("An error occurred: " + ctx.error());
            });
        });
        // app.get("/", ctx -> ctx.result("WebSocket server is running on ws://localhost:7070/websocket"));

    }

    private static CanvasSession getCanvasSession(String participantID) {
        for(CanvasSession session : ACTIVE_CANVAS_SESSIONS){
            for( String participant :session.getSessionParticipants()){
                if(participant.equals(participantID)){
                    return session;
                }
            }
        }
        return null;
    }

    private static String generateCanvasSession(String ownerID){

        boolean isUniqueSessionCode;
        String sessionCode;

        do {
            sessionCode = generateSessionCode();
            isUniqueSessionCode = true;

            for (CanvasSession session : ACTIVE_CANVAS_SESSIONS) {
                if (session.sessionCode.equals(sessionCode)) {
                    isUniqueSessionCode = false;
                    break;
                }
            }
        }
        while (!isUniqueSessionCode);

        ACTIVE_CANVAS_SESSIONS.add(new CanvasSession(sessionCode, ownerID ));
        LOGGER.debug("Added new canvas session: " + sessionCode);

        return sessionCode;

    }

    private static String generateSessionCode(){
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
