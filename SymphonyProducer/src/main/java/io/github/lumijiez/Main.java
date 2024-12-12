package io.github.lumijiez;

public class Main {

    public static void main(String[] args) {
        Thread brokerThread = new Thread(BrokerConnector::connect);
        brokerThread.start();

        Thread ftpThread = new Thread(FTPClientConnector::new);
        ftpThread.start();

        try {
            brokerThread.join();
            ftpThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}