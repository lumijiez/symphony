package io.github.lumijiez.raft;

import io.github.lumijiez.app.NodeManager;
import io.github.lumijiez.data.models.NodeInfo;
import io.github.lumijiez.network.UdpMessageSender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Raft {
    private static final Random RANDOM = new Random();
    private final Logger logger = LogManager.getLogger(Raft.class);

    // Configuration Parameters
    private static final int MIN_ELECTION_TIMEOUT = 300;
    private static final int MAX_ELECTION_TIMEOUT = 600;
    private static final int HEARTBEAT_INTERVAL = 100;
    private static final int QUORUM_FACTOR = 2;

    // State Variables
    private RaftStates state = RaftStates.FOLLOWER;
    private int currentTerm = 0;
    private String votedFor = null;
    private Set<String> votesReceived = new HashSet<>();

    private final NodeManager nodeManager;
    private final UdpMessageSender sender;
    private final ScheduledExecutorService electionExecutor = Executors.newSingleThreadScheduledExecutor();

    public Raft(NodeManager nodeManager, UdpMessageSender sender) {
        this.nodeManager = nodeManager;
        this.sender = sender;
        nodeManager.getNodes().removeIf(node -> node.hostname().equals(NodeManager.HOST) && node.port() == NodeManager.PORT);

        start();
    }

    public void start() {
        logger.info("Raft initialization. Total peers: {}", nodeManager.getNodes().size());
        becomeFollower(1);
    }

    private void becomeFollower(int term) {
        if (term > currentTerm) {
            state = RaftStates.FOLLOWER;
            currentTerm = term;
            votedFor = null;
            votesReceived.clear();
            logger.debug("Transitioned to FOLLOWER. Term: {}", term);
        }
        scheduleElectionTimeout();
    }

    private void becomeCandidate() {
        state = RaftStates.CANDIDATE;
        currentTerm++;
        votedFor = NodeManager.HOST + ":" + NodeManager.PORT;
        votesReceived = new HashSet<>(Set.of(votedFor)); // Self vote

      //  logger.info("Starting election in Term {}. Attempting to collect votes.", currentTerm);
        sendVoteRequests();
    }

    private void becomeLeader() {
        if (state == RaftStates.CANDIDATE &&
                votesReceived.size() >= (nodeManager.getNodes().size() / QUORUM_FACTOR + 1)) {
            state = RaftStates.LEADER;
            logger.info("LEADER ELECTION SUCCESS: Becoming LEADER in Term {}", currentTerm);
            votesReceived.clear();
            sendHeartbeats();
        }
    }

    private void sendVoteRequests() {
        String voteRequest = String.format("REQUEST_VOTE|%d|%s:%d",
                currentTerm, NodeManager.HOST, NodeManager.PORT);

        for (NodeInfo peer : nodeManager.getNodes()) {
            sender.sendMessage(peer, voteRequest);
        }
    }

    private void sendHeartbeats() {
        if (state != RaftStates.LEADER) return;

        String heartbeat = String.format("HEARTBEAT|%d|%s:%d",
                currentTerm, NodeManager.HOST, NodeManager.PORT);

        for (NodeInfo peer : nodeManager.getNodes()) {
            sender.sendMessage(peer, heartbeat);
        }

        // Reschedule heartbeats
        electionExecutor.schedule(this::sendHeartbeats, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private void scheduleElectionTimeout() {
        int timeout = MIN_ELECTION_TIMEOUT + RANDOM.nextInt(MAX_ELECTION_TIMEOUT - MIN_ELECTION_TIMEOUT);

        electionExecutor.schedule(() -> {
            if (state != RaftStates.LEADER) {
                logger.debug("Election timeout triggered. Starting new election.");
                becomeCandidate();
            }
        }, timeout, TimeUnit.MILLISECONDS);
    }

    public void processMessage(String message) {
        String[] parts = message.split("\\|");
        if (parts.length < 3) return;

        try {
            String type = parts[0];
            int messageTerm = Integer.parseInt(parts[1]);
            String sender = parts[2];

            // Term comparison and potential state change
            if (messageTerm > currentTerm) {
                becomeFollower(messageTerm);
            }

            switch (type) {
                case "REQUEST_VOTE":
                    handleRequestVote(messageTerm, sender);
                    break;
                case "HEARTBEAT":
                    handleHeartbeat(messageTerm, sender);
                    break;
                case "VOTE_GRANTED":
                    handleVoteGranted(messageTerm, sender);
                    break;
            }
        } catch (NumberFormatException e) {
            logger.error("Error processing message: {}", message);
        }
    }

    private void handleRequestVote(int term, String candidate) {
        boolean voteGranted = false;

        if (term >= currentTerm &&
                (votedFor == null || votedFor.equals(candidate))) {
            voteGranted = true;
            votedFor = candidate;
            currentTerm = term;
            scheduleElectionTimeout();
        }

        String response = String.format("VOTE_GRANTED|%d|%s", term, voteGranted);
        sender.sendMessage(NodeInfo.fromString(candidate), response);
    }

    private void handleHeartbeat(int term, String leader) {
        if (term >= currentTerm) {
            scheduleElectionTimeout();
            becomeFollower(term);
        }
    }

    private void handleVoteGranted(int term, String candidate) {
        if (state == RaftStates.CANDIDATE && term == currentTerm) {
            votesReceived.add(candidate);
            logger.debug("Vote received from {}. Total votes: {}/{}",
                    candidate, votesReceived.size(), (nodeManager.getNodes().size() / QUORUM_FACTOR + 1));

            becomeLeader();
        }
    }

    public void shutdown() {
        electionExecutor.shutdownNow();
    }
}