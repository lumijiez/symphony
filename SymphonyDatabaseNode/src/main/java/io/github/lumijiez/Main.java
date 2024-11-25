package io.github.lumijiez;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.github.lumijiez.data.Data;
import io.github.lumijiez.data.models.NodeInfo;
import jakarta.persistence.EntityManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;

public class Main {
    public static Logger logger = LogManager.getLogger(Main.class);
    private static final CountDownLatch waitForConnection = new CountDownLatch(1);
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        try {
            String hostname = System.getenv().getOrDefault("HOSTNAME", "localhost");
            int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

            logger.info("Node started");
            EntityManager em = Data.getEntityManager();

            logger.info("Connected to database:\u001B[33m\033[1m symphony");
            em.close();

            try (HttpClient client = HttpClient.newHttpClient()) {
                CompletableFuture<WebSocket> wsFuture = client
                        .newWebSocketBuilder()
                        .buildAsync(new URI("ws://symphony-discovery:8083/discovery"), new WebSocket.Listener() {
                            @Override
                            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                                try {
                                    Type nodeListType = new TypeToken<List<NodeInfo>>() {}.getType();
                                    List<NodeInfo> nodes = gson.fromJson(data.toString(), nodeListType);
////                                    logger.info("Discovered Nodes (JSON):\n{}",
////                                            new GsonBuilder()
////                                                    .setPrettyPrinting()
////                                                    .create()
////                                                    .toJson(nodes)
//                                    );
                                    logger.info("Acknowledged nodes:\u001B[33m\033[1m {}\u001B[0m", nodes.size());
                                } catch (NumberFormatException e) {
                                    logger.error("Received invalid node count: {}", data);
                                }
                                return WebSocket.Listener.super.onText(webSocket, data, last);
                            }

                            @Override
                            public void onOpen(WebSocket webSocket) {
                                NodeInfo nodeInfo = new NodeInfo(hostname, port);
                                webSocket.sendText(gson.toJson(nodeInfo), true);
                                logger.info("Successfully registered to \033[1mDiscovery");
                                waitForConnection.countDown();
                                WebSocket.Listener.super.onOpen(webSocket);
                            }

                            @Override
                            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                                logger.info("Unregistered from Discovery: {}", reason);
                                return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                            }

                            @Override
                            public void onError(WebSocket webSocket, Throwable error) {
                                logger.error("Error: {}", error.getMessage());
                                WebSocket.Listener.super.onError(webSocket, error);
                            }
                        });

                WebSocket ws = wsFuture.join();

                try {
                    waitForConnection.await();
                    Thread.currentThread().join();
                } finally {
                    ws.sendClose(WebSocket.NORMAL_CLOSURE, "Node shutting down").join();
                }
            }

        } catch (Exception e) {
            logger.error("Error in main: {}", e.getMessage(), e);
        }
    }
}