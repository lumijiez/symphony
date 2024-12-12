package io.github.lumijiez;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.net.ftp.FTPClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class FTPProducer {
    String ftpServer = "ftp_server";
    int ftpPort = 21;
    String ftpUser = "symphony";
    String ftpPass = "symphony";
    String ftpDir = "/";

    FTPProducer() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                uploadRandomJsonToFtp(ftpServer, ftpPort, ftpUser, ftpPass, ftpDir);
            }
        }, 0, 5000);
    }

    public static void uploadRandomJsonToFtp(String ftpServer, int ftpPort, String ftpUser, String ftpPass, String ftpDir) {
        JsonObject json = generateRandomJson();

        Gson gson = new Gson();
        String jsonString = gson.toJson(json);
        InputStream inputStream = new ByteArrayInputStream(jsonString.getBytes());

        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(ftpServer, ftpPort);
            boolean login = ftpClient.login(ftpUser, ftpPass);
            if (!login) {
                Main.logger.error("FTP login failed.");
                return;
            }

            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();

            String subDir = "uploads";
            if (!ftpClient.changeWorkingDirectory(subDir)) {
                Main.logger.info("Subdirectory does not exist. Creating: {}", subDir);
                boolean dirCreated = ftpClient.makeDirectory(subDir);
                if (!dirCreated) {
                    Main.logger.error("Failed to create subdirectory: {}", subDir);
                    Main.logger.error("Server reply: {}", ftpClient.getReplyString());
                    return;
                }
                ftpClient.changeWorkingDirectory(subDir);
            }

            String filename = "file_" + System.currentTimeMillis() + ".json";
            boolean uploaded = ftpClient.storeFile(filename, inputStream);
            if (uploaded) {
                Main.logger.info("Successfully uploaded: {}", filename);
            } else {
                Main.logger.error("Failed to upload: {}", filename);
                Main.logger.error("Server reply: {}", ftpClient.getReplyString());
            }

            ftpClient.logout();
        } catch (IOException e) {
            Main.logger.error("Error during FTP operation: {}", e.getMessage());
            Main.logger.error(e.getMessage());
        } finally {
            try {
                inputStream.close();
                if (ftpClient.isConnected()) {
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                Main.logger.error(e.getMessage());
            }
        }
    }

    public static JsonObject generateRandomJson() {
        Random rand = new Random();
        JsonObject json = new JsonObject();

        json.addProperty("id", rand.nextInt(1000));
        json.addProperty("name", "RandomUser" + rand.nextInt(1000));
        json.addProperty("email", "user" + rand.nextInt(1000) + "@example.com");
        json.addProperty("active", rand.nextBoolean());

        return json;
    }
}
