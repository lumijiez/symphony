package io.github.lumijiez.data.models;

public record NodeInfo(String hostname, int port) {
    public static NodeInfo fromString(String nodeString) {
        String[] parts = nodeString.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid node string format. Expected 'hostname:port'");
        }
        return new NodeInfo(parts[0], Integer.parseInt(parts[1]));
    }

    @Override
    public String toString() {
        return hostname + ":" + port;
    }
}
