package io.github.lumijiez;

import io.javalin.Javalin;
import io.javalin.websocket.WsContext;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class JavalinConfig {
    private static final Map<String, WsContext> users = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void setup(Javalin app) {        app.ws("/discovery", ws -> {
           ws.onConnect(ctx -> {
               String id = ctx.sessionId();
               users.put(id, ctx);
               broadcast("Discovery-Join", "Join");
           });

           ws.onClose(ctx -> {
               String id = ctx.sessionId();
               users.remove(id);
               broadcast("Discovery-Leave", "Leave");
           });
        });
    }

    private static void broadcast(String sender, String message) {
        users.values().forEach(ctx -> ctx.send(users.size()));
    }
}
