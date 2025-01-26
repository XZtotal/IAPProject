package iap.group1;

public class NotasMiddleware {

    public static final String DESTINATION_EXCHANGE = "notas.alumno.anyo";
    public static final String SOURCE_TOPIC_CSV = "generador.csv";
    public static final String SOURCE_TOPIC_JSON = "generador.json";
    public static final String SOURCE_EXCHANGE = "primer-exchange";

    public static final String HOST = "localhost";
    public static final int PORT = 5672;
    public static final String USERNAME = "guest";
    public static final String PASSWORD = "guest";



    public static void main(String[] args) {
        System.out.println("\n" +
                "  _   _       _            __  __ _     _     _ _                             \n" +
                " | \\ | |     | |          |  \\/  (_)   | |   | | |                            \n" +
                " |  \\| | ___ | |_ __ _ ___| \\  / |_  __| | __| | | _____      ____ _ _ __ ___ \n" +
                " | . ` |/ _ \\| __/ _` / __| |\\/| | |/ _` |/ _` | |/ _ \\ \\ /\\ / / _` | '__/ _ \\\n" +
                " | |\\  | (_) | || (_| \\__ \\ |  | | | (_| | (_| | |  __/\\ V  V / (_| | | |  __/\n" +
                " |_| \\_|\\___/ \\__\\__,_|___/_|  |_|_|\\__,_|\\__,_|_|\\___| \\_/\\_/ \\__,_|_|  \\___|\n" +
                "                                                                              \n" +
                "                                                                              \n");

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