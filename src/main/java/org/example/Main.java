package org.example;

import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    Logger LOGGER = LogManager.getLogger();

    // Ensures that adding, removing, or iterating over the user's
    // set happens safely when accessed by multiple threads. (Normal HashSet could lead to corruption)
    // A set only allows one unique entry, we don't want the same session twice.

    private static Set<WsContext> users = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(8888);

        // WebSocket endpoints
        app.ws("/websocket", ws -> {
            ws.onConnect(ctx -> {
                // TODO: In the future check the url params and create a new session
                //       if that url doesn't exist already, if it does connect to it
                System.out.println("User: " + ctx.sessionId() + "connected.");
                users.add(ctx); // Add the user to the set
            });

            ws.onMessage(ctx -> {
                System.out.println("Received message: " + ctx.message());
                
                // Broadcast the message to all users
                for (WsContext user : users) {
                    user.send("Handles message" + ctx.message());
                }
            });

            ws.onClose(ctx -> {
                System.out.println("User disconnected with session ID: " + ctx.sessionId());
                users.remove(ctx); // Remove the user from the set
            });

            ws.onError(ctx -> {
                System.out.println("An error occurred: " + ctx.error());
            });
        });

        app.get("/", ctx -> ctx.result("WebSocket server is running on ws://localhost:7070/websocket"));
    }
}
