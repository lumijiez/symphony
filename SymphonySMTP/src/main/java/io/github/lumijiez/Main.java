package io.github.lumijiez;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    public static void main(String[] args) {
        Logger logger = LogManager.getLogger(Main.class);
        logger.info("SMTP server started");

        String host = "smtp.gmail.com";
        String port = "587";
        String fromEmail = "your-email@gmail.com";
        String fromPassword = "your-email-password";
        String toEmail = "daniil.schipschi@isa.utm.md";
    }
}