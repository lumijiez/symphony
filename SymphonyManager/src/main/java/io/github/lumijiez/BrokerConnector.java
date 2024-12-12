package io.github.lumijiez;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static io.github.lumijiez.Main.logger;

public class BrokerConnector {
    private static final String QUEUE_NAME = "random_sha";
    private static final String RABBITMQ_HOST = "rabbitmq";
    private static final String RABBITMQ_USER = "symphony";
    private static final String RABBITMQ_PASSWORD = "symphony";
    private static final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private static final Logger logger = LogManager.getLogger(BrokerConnector.class);

    public static void connect() {
        try (ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor()) {
            reconnectExecutor.scheduleWithFixedDelay(() -> {
                try {
                    connectToRabbitMQ();
                } catch (Exception e) {
                    logger.warn("Awaiting broker connection: {}", e.getMessage());
                }
            }, 0, 5, TimeUnit.SECONDS);

            shutdownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Connector interrupted: {}", e.getMessage());
        }
    }

    private static void connectToRabbitMQ() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);
        factory.setUsername(RABBITMQ_USER);
        factory.setPassword(RABBITMQ_PASSWORD);

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            logger.info("Connected to RabbitMQ and queue declared");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println("Received message: " + message);

                try {
                    sendPostRequest(AddressHolder.getInstance().getHost(), AddressHolder.getInstance().getPort(), message);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            };

            channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});

            shutdownLatch.await();
        }
    }

    private static void sendPostRequest(String host, int port, String data) throws Exception {
        URL url = new URL("http://" + host + ":" + port + "/push");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "text/plain");
        connection.setDoOutput(true);

        byte[] postData = data.getBytes(StandardCharsets.UTF_8);
        connection.getOutputStream().write(postData);

        int responseCode = connection.getResponseCode();
        logger.info("POST to {}:{}/push with data: {}, Response Code: {}", host, port, data, responseCode);

        connection.getInputStream().close();
    }

}