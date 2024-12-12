package io.github.lumijiez;

import io.github.lumijiez.data.Data;
import io.github.lumijiez.data.entities.PushData;
import io.github.lumijiez.raft.Raft;
import io.javalin.Javalin;
import io.javalin.http.Context;
import jakarta.persistence.EntityManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    public static final String HOST = System.getenv().getOrDefault("HOSTNAME", "localhost");
    public static final int PORT = Integer.parseInt(System.getenv().getOrDefault("UDP_PORT", "8084"));
    public static final int HTTP_PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        Thread raftThread = new Thread(() -> {
            Raft raft = new Raft();
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Raft thread interrupted", e);
            }
        });
        raftThread.start();

        Javalin app = Javalin.create().start(HTTP_PORT);

        app.post("/push", Main::handlePush);

        logger.info("HTTP server started on port {}", HTTP_PORT);
    }

    private static void handlePush(Context ctx) {
        String data = ctx.body();
        logger.info("Received data for push: {}", data);

        EntityManager entityManager = Data.getEntityManager();
        try {
            entityManager.getTransaction().begin();

            PushData entity = new PushData(HOST + ":" + PORT, data);
            entityManager.persist(entity);

            entityManager.getTransaction().commit();
            ctx.status(200).result("Data pushed successfully");
        } catch (Exception e) {
            logger.error("Error saving data to database", e);
            entityManager.getTransaction().rollback();
            ctx.status(500).result("Internal Server Error");
        } finally {
            entityManager.close();
        }
    }
}
