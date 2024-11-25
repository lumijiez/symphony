package io.github.lumijiez.network;

import io.github.lumijiez.app.NodeManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

public class UdpListener {
    private static final Logger logger = LogManager.getLogger(UdpListener.class);

    private final NodeManager nodeManager;

    public UdpListener(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    public void startListening() {
        Thread udpListenerThread = new Thread(() -> {
            try (Selector selector = Selector.open();
                 DatagramChannel channel = DatagramChannel.open()) {

                channel.bind(new InetSocketAddress(NodeManager.PORT));
                channel.configureBlocking(false);
                channel.register(selector, SelectionKey.OP_READ);

                logger.info("UDP listens on port {}", NodeManager.PORT);
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
                            logger.info("Received UDP {}:{}: {}", sender.getHostName(), sender.getPort(), message);
                            nodeManager.handleMessage(message);
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Error in UDP listener: {}", e.getMessage());
            }
        });
        udpListenerThread.start();
    }
}