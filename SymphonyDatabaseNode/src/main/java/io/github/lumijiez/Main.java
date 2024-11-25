package io.github.lumijiez;


import io.github.lumijiez.app.NodeManager;

public class Main {

    public static void main(String[] args) {
        NodeManager manager = new NodeManager();

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
