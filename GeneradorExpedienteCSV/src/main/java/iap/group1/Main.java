package iap.group1;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import dao.AlumnoAsignaturaDAO;
import dao.AlumnoDAO;
import dao.DAOFactory;
import domain.Alumno;
import domain.AlumnoAsignatura;

public class Main {
    private static final String RABBITMQ_BROKER = "localhost";
    private static final String NOMBRE_EXCHANGE = "exchange_name";
    private static final String TOPIC = "exchange_name";

    public static void main(String[] args) throws IOException, TimeoutException {
        String id = "Producer-" + UUID.randomUUID();
        try (Connection connection = createConnection();
             Channel channel = createChannel(connection);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("[" + id + "] Connected to the broker RabbitMQ " + RABBITMQ_BROKER);
            declareExchange(channel, id);

            String message;
            do {
                System.out.print("Enter the student's DNI number or write exit to finish the program:");
                message = scanner.nextLine();
                if (!message.equalsIgnoreCase("exit")) {
                    processStudentData(channel, scanner, message);
                }
            } while (!message.equalsIgnoreCase("exit"));

            closeConnection(channel, connection, id);
        }
    }

    private static Connection createConnection() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_BROKER);
        return factory.newConnection();
    }

    private static Channel createChannel(Connection connection) throws IOException {
        return connection.createChannel();
    }

    private static void declareExchange(Channel channel, String id) throws IOException {
        channel.exchangeDeclare(NOMBRE_EXCHANGE, BuiltinExchangeType.TOPIC, false, true, null);
        System.out.println("[" + id + "] Declared a EXCHANGE called " + NOMBRE_EXCHANGE + " of type TOPIC");
    }

    private static void processStudentData(Channel channel, Scanner scanner, String dni) throws IOException {
        System.out.print("Enter year: ");
        int year = Integer.parseInt(scanner.nextLine());
        File csvFile = new File("Expediente_" + dni + ".csv");

        try (FileWriter writer = new FileWriter(csvFile)) {
            String studentData = fetchStudentData(dni, year);
            writer.write(studentData);
        }

        byte[] content = Files.readAllBytes(csvFile.toPath());
        channel.basicPublish(NOMBRE_EXCHANGE, TOPIC, null, content);
    }

    private static String fetchStudentData(String dni, int year) {
        DAOFactory daoFactory = DAOFactory.getCurrentInstance();
        daoFactory.connect("localhost", "3306", "root", "", "centroeducativo");

        AlumnoDAO alumnoDAO = daoFactory.getAlumnoDAO();
        AlumnoAsignaturaDAO alAsDAO = daoFactory.getAlumnoAsignaturaDAO();

        Alumno alumno = alumnoDAO.getAlumnoByDNI(dni);

        StringBuilder resultCsv = new StringBuilder();
        resultCsv.append("notas-alumno-anyo\n");
        resultCsv.append("format-01, version 1.0\n");
        resultCsv.append(alumno.getDni()).append(",").append(alumno.getNombre()).append(",").append(alumno.getApellidos()).append("\n");
        resultCsv.append(year).append("\n");

        List<AlumnoAsignatura> asignaturas = alAsDAO.getAlumnoAsignaturaByAnyo(dni, year);
        for (AlumnoAsignatura asignatura : asignaturas) {
            resultCsv.append(asignatura.getAsignatura()).append(", ").append(asignatura.getNota()).append("\n");
        }

        return resultCsv.toString();
    }

    private static void closeConnection(Channel channel, Connection connection, String id) throws IOException, TimeoutException {
        String message = "close";
        channel.basicPublish(NOMBRE_EXCHANGE, TOPIC, null, message.getBytes());
        connection.close();
        System.out.println("\n[" + id + "] connection closed!");
    }
}