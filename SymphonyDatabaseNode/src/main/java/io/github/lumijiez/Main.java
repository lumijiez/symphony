package io.github.lumijiez;

import jakarta.persistence.EntityManager;

public class Main {
    public static void main(String[] args) {
        System.out.println("Node up");
        EntityManager em = Data.getEntityManager();

        System.out.println("Connected to DB << symphony >>");
        em.close();
    }
}