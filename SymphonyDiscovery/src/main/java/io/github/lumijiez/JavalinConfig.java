package io.github.lumijiez;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.json.JavalinGson;
import io.javalin.websocket.WsContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JavalinConfig {
    private static final Map<String, WsContext> users = new ConcurrentHashMap<>();

    public static void setup(Javalin app) {
        app.get("/check", ctx -> ctx.result("OK"));

        app.ws("/discovery", ws -> {
           ws.onConnect(ctx -> {
               String id = ctx.sessionId();
               users.put(id, ctx);
               broadcast("Discovery", "Join");
           });

           ws.onClose(ctx -> {
               String id = ctx.sessionId();
               users.remove(id);
               broadcast("Discovery", "Leave");
           });
        });
    }

    private static void broadcast(String sender, String message) {
        users.values().forEach(ctx -> ctx.send(users.size()));
    }
}
