package io.github.lumijiez;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BrokerConnector {
    private static final String QUEUE_NAME = "random_json_queue";
    private static final String RABBITMQ_HOST = "rabbitmq";
    private static final String RABBITMQ_USER = "symphony";
    private static final String RABBITMQ_PASSWORD = "symphony";

    public static void connect() {
        ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();

        reconnectExecutor.scheduleWithFixedDelay(() -> {
            try {
                connectToRabbitMQ();
            } catch (Exception e) {
                System.err.println("Awaiting broker connection: " + e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    private static void connectToRabbitMQ() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);
        factory.setUsername(RABBITMQ_USER);
        factory.setPassword(RABBITMQ_PASSWORD);

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            System.out.println("Waiting for messages. To exit press CTRL+C");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                // String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            };

            channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});

            Thread.currentThread().join();
        }
    }

}
