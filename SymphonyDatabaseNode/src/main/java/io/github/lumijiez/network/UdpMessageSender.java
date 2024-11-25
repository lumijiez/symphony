package io.github.lumijiez.network;

import io.github.lumijiez.app.NodeManager;
import io.github.lumijiez.data.models.NodeInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class UdpMessageSender {
    private static final Logger logger = LogManager.getLogger(UdpMessageSender.class);
    private final NodeManager nodeManager;

    public UdpMessageSender(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    public void sendMessage(NodeInfo node, String message) {
        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.configureBlocking(false);
            ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
            channel.send(buffer, new InetSocketAddress(node.hostname(), node.port()));
            logger.info("Sent UDP to {}:{}: {}", node.hostname(), node.port(), message);
        } catch (IOException e) {
            logger.error("Error sending UDP message: {}", e.getMessage());
        }
    }

    public void sendCountMessage(NodeInfo node, int count) {
        sendMessage(node, String.valueOf(count));
    }
}