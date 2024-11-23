package io.github.lumijiez.models.requests;

public class RegisterRequest {
    public String hostname;
    public String port;

    public RegisterRequest(String hostname, String port) {
        this.hostname = hostname;
        this.port = port;
    }

    public String getHostname() {
        return hostname;
    }

    public String getPort() {
        return port;
    }
}
