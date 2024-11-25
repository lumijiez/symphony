package io.github.lumijiez.network;

import io.github.lumijiez.app.NodeManager;
import io.github.lumijiez.data.models.NodeInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CompletableFuture;
import java.lang.reflect.Type;

public class WebSocketManager {
    private static final Logger logger = LogManager.getLogger(WebSocketManager.class);
    private static final Gson gson = new Gson();
    private static final CountDownLatch waitForConnection = new CountDownLatch(1);

    private WebSocket webSocket;
    private final NodeManager nodeManager;

    public WebSocketManager(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    public void connectAndListen() {
        try (HttpClient client = HttpClient.newHttpClient()) {
            CompletableFuture<WebSocket> wsFuture = client
                    .newWebSocketBuilder()
                    .buildAsync(new URI("ws://symphony-discovery:8083/discovery"), new WebSocket.Listener() {
                        @Override
                        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                            try {
                                Type nodeListType = new TypeToken<List<NodeInfo>>() {}.getType();
                                List<NodeInfo> nodes = gson.fromJson(data.toString(), nodeListType);

                                for (NodeInfo newNode : nodes) {
                                    nodeManager.registerNode(newNode);
//                                    logger.info("Discovered node: {}:{}", newNode.hostname(), newNode.port());
                                }

                            } catch (Exception e) {
                                logger.error("Error processing WebSocket data: {}", e.getMessage());
                            }
                            return WebSocket.Listener.super.onText(webSocket, data, last);
                        }

                        @Override
                        public void onOpen(WebSocket webSocket) {
                            NodeInfo nodeInfo = new NodeInfo(NodeManager.HOST, NodeManager.PORT);
                            webSocket.sendText(gson.toJson(nodeInfo), true);
                            logger.info("Successfully registered to Discovery");
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
                            logger.error("WebSocket error: {}", error.getMessage());
                            WebSocket.Listener.super.onError(webSocket, error);
                        }
                    });

            webSocket = wsFuture.join();
            waitForConnection.await();

        } catch (Exception e) {
            logger.error("Error in WebSocketManager: {}", e.getMessage());
        }
    }

    public void send(NodeInfo nodeInfo) {
        if (webSocket != null) {
            webSocket.sendText(gson.toJson(nodeInfo), true);
        }
    }
}
