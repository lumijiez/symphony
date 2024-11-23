package io.github.lumijiez;

import io.github.lumijiez.data.Data;
import jakarta.persistence.EntityManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;

public class Main {
    public static Logger logger = LogManager.getLogger(Main.class);
    private static final CountDownLatch waitForConnection = new CountDownLatch(1);

    public static void main(String[] args) {
        try {
            logger.info("Node up.");
            EntityManager em = Data.getEntityManager();

            logger.info("Connected to database: << symphony >>");
            em.close();

            try (HttpClient client = HttpClient.newHttpClient()) {
                CompletableFuture<WebSocket> wsFuture = client
                        .newWebSocketBuilder()
                        .buildAsync(new URI("ws://symphony-discovery:8083/discovery"), new WebSocket.Listener() {
                            @Override
                            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                                try {
                                    int nodeCount = Integer.parseInt(data.toString());
                                    logger.info("Acknowledged system nodes: {}", nodeCount);
                                } catch (NumberFormatException e) {
                                    logger.error("Received invalid node count: {}", data);
                                }
                                return WebSocket.Listener.super.onText(webSocket, data, last);
                            }

                            @Override
                            public void onOpen(WebSocket webSocket) {
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