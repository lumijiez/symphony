package io.github.lumijiez;

import jakarta.persistence.EntityManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class Main {
    public static Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Node up.");
        EntityManager em = Data.getEntityManager();

        logger.info("Connected to database: << symphony >>");
        em.close();
    }
}