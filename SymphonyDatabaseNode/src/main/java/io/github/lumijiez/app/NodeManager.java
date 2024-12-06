package io.github.lumijiez.app;

import io.github.lumijiez.data.models.NodeInfo;
import io.github.lumijiez.network.UdpListener;
import io.github.lumijiez.network.UdpMessageSender;
import io.github.lumijiez.network.WebSocketManager;
import io.github.lumijiez.raft.Raft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class NodeManager {
    private final Logger logger = LogManager.getLogger(NodeManager.class);

    private final UdpListener listener;
    private final UdpMessageSender sender;
    private final WebSocketManager ws;
    private final Raft raft;
    private final List<NodeInfo> nodes = new ArrayList<>();
    public static final String HOST = System.getenv().getOrDefault("HOSTNAME", "localhost");
    public static final int PORT = Integer.parseInt(System.getenv().getOrDefault("UDP_PORT", "8084"));

    public NodeManager() {
        this.listener = new UdpListener(this);
        this.sender = new UdpMessageSender(this);
        this.ws = new WebSocketManager(this);
        this.raft = new Raft(this, sender);

        listener.startListening();
        ws.connectAndListen();
        raft.start();
    }

    public void handleMessage(String message) {
        raft.processMessage(message);
    }

    public List<NodeInfo> getNodes() {
        return nodes;
    }

    public void registerNode(NodeInfo node) {
        if (!nodes.contains(node)) {
            nodes.add(node);
            sender.sendMessage(node, "NODE_REGISTERED");
          //  logger.info("New node registered, updating Raft peers");
        }
    }

    public void removeNode(NodeInfo node) {
        nodes.remove(node);
      //  logger.info("Node removed, updating Raft peers");
    }

    public UdpMessageSender getSender() {
        return sender;
    }
}