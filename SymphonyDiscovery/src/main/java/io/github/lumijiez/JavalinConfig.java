package io.github.lumijiez;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JavalinConfig {
    private static final Map<String, NodeInfo> registeredNodes = new ConcurrentHashMap<>();
    private static final Map<String, WsContext> nodes = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();
    private static final Logger logger = LogManager.getLogger(JavalinConfig.class);

    public static void setup(Javalin app) {
        app.ws("/discovery", ws -> {
            ws.onConnect(ctx -> {
                // ToDo
                // A general notification
                nodes.put(ctx.sessionId(), ctx);
            });

            ws.onMessage(ctx -> {
                String message = ctx.message();
                NodeInfo nodeInfo = gson.fromJson(message, NodeInfo.class);
                registeredNodes.put(ctx.sessionId(), nodeInfo);
                broadcastNodeCount();
            });

            ws.onClose(ctx -> {
                registeredNodes.remove(ctx.sessionId());
                broadcastNodeCount();
            });
        });
    }

    private static void broadcastNodeCount() {
        List<NodeInfo> nodeInfoList = new ArrayList<>(registeredNodes.values());
        String nodesJson = gson.toJson(nodeInfoList);
        nodes.values().forEach(ctx -> ctx.send(nodesJson));
    }

    public record NodeInfo(String hostname, int port) { }
}
