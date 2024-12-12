package io.github.lumijiez;

import io.github.lumijiez.broker.BrokerConnector;
import io.github.lumijiez.ftp.FTPProducer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

    public static Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        Thread brokerThread = new Thread(BrokerConnector::connect);
        brokerThread.start();

        Thread ftpThread = new Thread(FTPProducer::new);
        ftpThread.start();

        try {
            brokerThread.join();
            ftpThread.join();
        } catch (InterruptedException e) {
            Main.logger.error(e.getMessage());
        }
    }
}