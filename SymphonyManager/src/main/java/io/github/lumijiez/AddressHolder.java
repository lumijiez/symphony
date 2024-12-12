package io.github.lumijiez;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static io.github.lumijiez.Main.logger;

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

