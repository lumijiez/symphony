package io.github.lumijiez;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BrokerConnector {
    private static final String QUEUE_NAME = "random_sha";
    private static final String RABBITMQ_HOST = "rabbitmq";
    private static final String RABBITMQ_USER = "symphony";
    private static final String RABBITMQ_PASSWORD = "symphony";
    private static final Logger logger = LogManager.getLogger(BrokerConnector.class);

    public static void connect() {
        CountDownLatch latch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received");
            latch.countDown();
        }));

        boolean success = connectToRabbitMQ(latch);
        logger.info("Success: {}", success);
    }

    private static boolean connectToRabbitMQ(CountDownLatch latch) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);
        factory.setUsername(RABBITMQ_USER);
        factory.setPassword(RABBITMQ_PASSWORD);

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel();
             ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor()) {

            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            logger.info("Connected to RabbitMQ and queue declared");

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    String jsonMessage = generateRandomSHA256();
                    channel.basicPublish("", QUEUE_NAME, null, jsonMessage.getBytes(StandardCharsets.UTF_8));
                    System.out.println("Sent: " + jsonMessage);
                } catch (IOException e) {
                    logger.error("Failed to send message: {}", e.getMessage());
                }
            }, 0, 1, TimeUnit.SECONDS);

            latch.await();
            scheduler.shutdown();

            return scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("Awaiting broker connection: {}", e.getMessage());
            try {
                Thread.sleep(5000);
                connectToRabbitMQ(latch);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        return false;
    }

    private static String generateRandomSHA256() {
        try {
            Random random = new Random();
            byte[] randomBytes = new byte[32];
            random.nextBytes(randomBytes);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(randomBytes);

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
