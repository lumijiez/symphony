package io.github.lumijiez;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.lumijiez.data.Data;
import io.github.lumijiez.data.models.NodeInfo;
import jakarta.persistence.EntityManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;

public class Main {
    public static Logger logger = LogManager.getLogger(Main.class);
    private static final CountDownLatch waitForConnection = new CountDownLatch(1);
    private static final Gson gson = new Gson();
    private static final List<NodeInfo> knownNodes = new ArrayList<>();
    private static int nodeCount = 0;

    public static void main(String[] args) {
        try {
            String hostname = System.getenv().getOrDefault("HOSTNAME", "localhost");
            int udpPort = Integer.parseInt(System.getenv().getOrDefault("UDP_PORT", "8084"));

            startUdpListener(udpPort);

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

                                    for (NodeInfo newNode : nodes) {
                                        if (!knownNodes.contains(newNode)) {
                                            knownNodes.add(newNode);
                                            nodeCount++;
                                            sendUdpSignal(newNode);
                                        }
                                    }

                                    logger.info("Acknowledged nodes:\u001B[33m\033[1m {}\u001B[0m", knownNodes.size());
                                } catch (Exception e) {
                                    logger.error("Error processing nodes: {}", e.getMessage());
                                }
                                return WebSocket.Listener.super.onText(webSocket, data, last);
                            }

                            @Override
                            public void onOpen(WebSocket webSocket) {
                                NodeInfo nodeInfo = new NodeInfo(hostname, udpPort);
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

    private static void sendUdpSignal(NodeInfo node) {
        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.configureBlocking(false);
            String message = String.valueOf(nodeCount);
            ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
            channel.send(buffer, new InetSocketAddress(node.hostname(), node.port()));
            logger.info("Sent UDP: \033[1m{}, message: \u001B[33m\033[1m{}", node.hostname() + ":" + node.port(), message);
        } catch (IOException e) {
            logger.error("Error sending UDP signal: {}", e.getMessage());
        }
    }

    private static void startUdpListener(int udpPort) {
        Thread udpListenerThread = new Thread(() -> {
            try (Selector selector = Selector.open();
                 DatagramChannel channel = DatagramChannel.open()) {

                channel.bind(new InetSocketAddress(udpPort));
                channel.configureBlocking(false);
                channel.register(selector, SelectionKey.OP_READ);

                logger.info("UDP listens on port \033[1m{}", udpPort);
                ByteBuffer buffer = ByteBuffer.allocate(1024);

                while (!Thread.currentThread().isInterrupted()) {
                    selector.select();
                    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                    while (keys.hasNext()) {
                        SelectionKey key = keys.next();
                        keys.remove();

                        if (key.isReadable()) {
                            DatagramChannel datagramChannel = (DatagramChannel) key.channel();
                            buffer.clear();
                            InetSocketAddress sender = (InetSocketAddress) datagramChannel.receive(buffer);
                            buffer.flip();

                            String message = new String(buffer.array(), 0, buffer.limit()).trim();
                            logger.info("Received UDP: \033[1m{}:{}: \u001B[33m\033[1m{}",
                                    sender.getHostName(),
                                    sender.getPort(),
                                    message);
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("UDP listener error: {}", e.getMessage());
            }
        });
        udpListenerThread.start();
    }
}
