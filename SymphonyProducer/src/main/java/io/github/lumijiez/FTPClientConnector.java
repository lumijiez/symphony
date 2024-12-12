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

public class FTPClientConnector {
    String ftpServer = "ftp_server";
    int ftpPort = 21;
    String ftpUser = "symphony";
    String ftpPass = "symphony";
    String ftpDir = "/";

    FTPClientConnector() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                uploadRandomJsonToFtp(ftpServer, ftpPort, ftpUser, ftpPass, ftpDir);
            }
        }, 0, 30000);
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
                System.out.println("FTP login failed.");
                return;
            }

            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();

            String subDir = "uploads";
            if (!ftpClient.changeWorkingDirectory(subDir)) {
                System.out.println("Subdirectory does not exist. Creating: " + subDir);
                boolean dirCreated = ftpClient.makeDirectory(subDir);
                if (!dirCreated) {
                    System.out.println("Failed to create subdirectory: " + subDir);
                    System.out.println("Server reply: " + ftpClient.getReplyString());
                    return;
                }
                ftpClient.changeWorkingDirectory(subDir);
            }

            String filename = "file_" + System.currentTimeMillis() + ".json";
            boolean uploaded = ftpClient.storeFile(filename, inputStream);
            if (uploaded) {
                System.out.println("Successfully uploaded: " + filename);
            } else {
                System.out.println("Failed to upload: " + filename);
                System.out.println("Server reply: " + ftpClient.getReplyString());
            }

            ftpClient.logout();
        } catch (IOException e) {
            System.out.println("Error during FTP operation: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
                if (ftpClient.isConnected()) {
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
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
