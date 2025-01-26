package iap.group1;

public class NotasMiddleware {

    public static void main(String[] args) {
        Thread csvConsumerThread = new Thread(new CSVConsumer());
        Thread jsonConsumerThread = new Thread(new JSONConsumer());

        csvConsumerThread.start();
        jsonConsumerThread.start();

        try {
            csvConsumerThread.join();
            jsonConsumerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}