package io.github.lumijiez;

import jakarta.persistence.EntityManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Main {
    public static Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        logger.info("Node up.");
        EntityManager em = Data.getEntityManager();

        logger.info("Connected to database: << symphony >>");
        em.close();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://symphony-discovery:8083/check"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            logger.info("Node successfully registered to Discovery");
        }
    }
}