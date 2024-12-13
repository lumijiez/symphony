package io.github.lumijiez.ftp;

import io.github.lumijiez.Main;
import io.github.lumijiez.http.AddressHolder;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class FTPFetcher {
    String ftpServer = "ftp_server";
    int ftpPort = 21;
    String ftpUser = "symphony";
    String ftpPass = "symphony";
    String ftpDir = "/uploads";

    public FTPFetcher() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                fetchAndDeleteFilesFromFtp(ftpServer, ftpPort, ftpUser, ftpPass, ftpDir);
            }
        }, 0, 5000);
    }

    public static void fetchAndDeleteFilesFromFtp(String ftpServer, int ftpPort, String ftpUser, String ftpPass, String ftpDir) {
        FTPClient ftpClient = new FTPClient();

        try {
            ftpClient.connect(ftpServer, ftpPort);
//            ftpClient.setControlKeepAliveTimeout(300);
            boolean login = ftpClient.login(ftpUser, ftpPass);
            if (!login) {
                Main.logger.error("FTP login failed.");
                return;
            }

            ftpClient.enterLocalPassiveMode();
            ftpClient.changeWorkingDirectory(ftpDir);

            String[] fileNames = ftpClient.listNames();
            if (fileNames == null || fileNames.length == 0) {
                Main.logger.error("No files found in the directory.");
                return;
            }

            for (String fileName : fileNames) {
                File localFile = new File("downloaded_" + fileName);
                try (FileOutputStream fos = new FileOutputStream(localFile)) {
                    boolean success = ftpClient.retrieveFile(fileName, fos);
                    if (success) {
                        Main.logger.info("File downloaded: {}", localFile.getName());

                        sendFileToMultipartEndpoint(localFile,
                                "http://"
                                        + AddressHolder.getInstance().getHost()
                                        + ":" + AddressHolder.getInstance().getPort()
                                        + "/upload", "Description of " + fileName);
                    } else {
                        Main.logger.error("Failed to download the file: {}", fileName);
                    }
                }

                boolean deleted = ftpClient.deleteFile(fileName);
                if (deleted) {
                    Main.logger.info("File deleted from FTP server: {}", fileName);
                } else {
                    Main.logger.error("Failed to delete the file from FTP server: {}", fileName);
                }
            }
        } catch (IOException e) {
            Main.logger.error(e.getMessage());
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                Main.logger.error(ex.getMessage());
            }
        }
    }

    private static void sendFileToMultipartEndpoint(File file, String endpointUrl, String description) {
        try {
            String boundary = UUID.randomUUID().toString();

            HttpURLConnection connection = (HttpURLConnection) new URL(endpointUrl).openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream outputStream = connection.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true)) {

                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"description\"\r\n");
                writer.append("Content-Type: text/plain; charset=UTF-8\r\n\r\n");
                writer.append(description).append("\r\n");

                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                        .append(file.getName()).append("\"\r\n");
                writer.append("Content-Type: application/octet-stream\r\n\r\n");
                writer.flush();

                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }

                writer.append("\r\n--").append(boundary).append("--\r\n");
                writer.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                System.out.println("File successfully uploaded to the endpoint " + AddressHolder.getInstance().getHost() + ":" + AddressHolder.getInstance().getPort() + ": " + file.getName());
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    System.out.println("Server response: " + response);
                }
            } else {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    String line;
                    StringBuilder errorResponse = new StringBuilder();
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    System.err.println("Failed to upload file. Response code: " + responseCode);
                    System.err.println("Error details: " + errorResponse);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error sending file to endpoint: " + e.getMessage());
        }
    }
}
