package io.github.lumijiez;

import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

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
}