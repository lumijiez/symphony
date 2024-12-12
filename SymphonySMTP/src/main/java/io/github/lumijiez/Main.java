package io.github.lumijiez;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    public static Logger logger = LogManager.getLogger(Main.class);
    public static void main(String[] args) {
        try {
            SMTPSender emailSender = new SMTPSender(
                    "smtp.gmail.com",
                    587,
                    "danthevip@gmail.com",
                    "",
                    false
            );

            emailSender.sendEmail(
                    "danthevip@gmail.com",
                    "daniil.schipschi@isa.utm.md",
                    "Test",
                    "Test test test. Hehehehehehehehehehehehehehehehehehe!"
            );

            logger.info("Email sent successfully!");
        } catch (Exception e) {
            logger.error("Error sending email: {}", e.getMessage());
        }
    }
}