package io.github.lumijiez;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

public class Main {
    private static final String QUEUE_NAME = "random_json_queue";
    private static final String RABBITMQ_HOST = "rabbitmq";
    private static final String RABBITMQ_USER = "symphony";
    private static final String RABBITMQ_PASSWORD = "symphony";

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);
        factory.setUsername(RABBITMQ_USER);
        factory.setPassword(RABBITMQ_PASSWORD);

        Connection connection = null;
        Channel channel = null;

        while (true) {
            try {
                System.out.println("Attempting to connect to RabbitMQ...");

                connection = factory.newConnection();
                channel = connection.createChannel();

                channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                System.out.println("Connected to RabbitMQ and queue declared.");

                Timer timer = new Timer(true);
                Channel finalChannel = channel;
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            String jsonMessage = generateRandomJson();
                            finalChannel.basicPublish("", QUEUE_NAME, null, jsonMessage.getBytes(StandardCharsets.UTF_8));
                            System.out.println("Sent: " + jsonMessage);
                        } catch (IOException e) {
                            System.err.println("Failed to send message: " + e.getMessage());
                        }
                    }
                }, 0, 10000);

                System.out.println("Press Ctrl+C to exit.");
                Thread.sleep(Long.MAX_VALUE);
                break;

            } catch (IOException | TimeoutException | InterruptedException e) {
                System.err.println("Failed to connect to RabbitMQ: " + e.getMessage());
                System.err.println("Retrying in 5 seconds...");
                try {
                    if (connection != null && connection.isOpen()) connection.close();
                    if (channel != null && channel.isOpen()) channel.close();
                } catch (IOException | TimeoutException ignored) {
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
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
