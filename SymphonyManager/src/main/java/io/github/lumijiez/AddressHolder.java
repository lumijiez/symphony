package io.github.lumijiez;

public class AddressHolder {
    private String host;
    private int port;

    private static AddressHolder INSTANCE;

    public static AddressHolder getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AddressHolder();
        }
        return INSTANCE;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }
}

