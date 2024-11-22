package io.github.lumijiez;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BrokerConnector {
    private static final String QUEUE_NAME = "random_json_queue";
    private static final String RABBITMQ_HOST = "rabbitmq";
    private static final String RABBITMQ_USER = "symphony";
    private static final String RABBITMQ_PASSWORD = "symphony";

    public static void connect() {
        CountDownLatch latch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown signal received.");
            latch.countDown();
        }));

        boolean success = connectToRabbitMQ(latch);
        System.out.println("Success: " + success);
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
            System.out.println("Connected to RabbitMQ and queue declared.");

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    String jsonMessage = generateRandomJson();
                    channel.basicPublish("", QUEUE_NAME, null, jsonMessage.getBytes(StandardCharsets.UTF_8));
                    System.out.println("Sent: " + jsonMessage);
                } catch (IOException e) {
                    System.err.println("Failed to send message: " + e.getMessage());
                }
            }, 0, 10, TimeUnit.SECONDS);

            latch.await();
            scheduler.shutdown();

            return scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Awaiting broker connection: " + e.getMessage());
            try {
                Thread.sleep(5000);
                connectToRabbitMQ(latch);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        return false;
    }

    private static String generateRandomJson() {
        Random random = new Random();
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("id", new JsonPrimitive(random.nextInt(1000)));
        jsonObject.add("name", new JsonPrimitive("Item_" + random.nextInt(100)));
        jsonObject.add("value", new JsonPrimitive(random.nextDouble() * 100));

        return jsonObject.toString();
    }
}
