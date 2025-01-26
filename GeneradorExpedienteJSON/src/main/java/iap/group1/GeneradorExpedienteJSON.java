package iap.group1;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import dao.AlumnoAsignaturaDAO;
import dao.AlumnoDAO;
import dao.DAOFactory;
import domain.Alumno;
import domain.AlumnoAsignatura;



public class GeneradorExpedienteJSON {
    private static final String RABBITMQ_BROKER = "localhost";
    private static final String NOMBRE_EXCHANGE = "primer-exchange";
    private static final String TOPIC = "generador.json";

    public static void main(String[] args) throws IOException, TimeoutException, JSONException {

        System.out.println("   _____            ______                 _  _____  ____  _   _ \n" +
                   "  / ____|          |  ____|               | |/ ____|/ __ \\| \\ | |\n" +
                   " | |  __  ___ _ __ | |__  __  ___ __      | | (___ | |  | |  \\| |\n" +
                   " | | |_ |/ _ \\ '_ \\|  __| \\ \\/ / '_ \\ _   | |\\___ \\| |  | | . ` |\n" +
                   " | |__| |  __/ | | | |____ >  <| |_) | |__| |____) | |__| | |\\  |\n" +
                   "  \\_____|\\___|_| |_|______/_/\\_\\ .__/ \\____/|_____/ \\____/|_| \\_|\n" +
                   "                               | |                               \n" +
                   "                               |_|                               ");

        String id = "Producer-" + UUID.randomUUID();
        try (Connection connection = createConnection();
             Channel channel = createChannel(connection);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("[" + id + "] Connected to the broker RabbitMQ " + RABBITMQ_BROKER);
            declareExchange(channel, id);

            String message;
            do {
                System.out.print("[?] Enter the student's DNI or 'exit' to finish:");
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
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");
        return factory.newConnection();
    }

    private static Channel createChannel(Connection connection) throws IOException {
        return connection.createChannel();
    }

    private static void declareExchange(Channel channel, String id) throws IOException {
        channel.exchangeDeclare(NOMBRE_EXCHANGE, BuiltinExchangeType.TOPIC);
        System.out.println("[" + id + "] Declared a EXCHANGE called " + NOMBRE_EXCHANGE + " of type TOPIC");
    }

    private static void processStudentData(Channel channel, Scanner scanner, String dni) throws IOException, JSONException {
        try {
            System.out.print("[?] Enter year: ");
            int year = Integer.parseInt(scanner.nextLine());
    //        File jsonFile = new File("generadores" + File.separator + "Expediente_" + dni + ".json");
    //        jsonFile.getParentFile().mkdirs();
    //
    //        try (FileWriter writer = new FileWriter(jsonFile)) {
    //            JSONObject studentData = fetchStudentData(dni, year);
    //            writer.write(studentData.toString(4)); // Indentación de 4 espacios
    //        }
    //
    //        byte[] content = Files.readAllBytes(jsonFile.toPath());

            JSONObject studentData = fetchStudentData(dni, year);
            channel.basicPublish(NOMBRE_EXCHANGE, TOPIC, null, studentData.toString().getBytes());

        } catch (NumberFormatException e) {
            System.err.println("[!] Error: Invalid year format. Please enter a valid number.");
        } catch (Exception e) {
            System.err.println("[!] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static JSONObject fetchStudentData(String dni, int year) throws JSONException {
        DAOFactory daoFactory = DAOFactory.getCurrentInstance();
        daoFactory.connect("localhost", "3306", "root", "", "centroeducativo");

        AlumnoDAO alumnoDAO = daoFactory.getAlumnoDAO();
        AlumnoAsignaturaDAO alAsDAO = daoFactory.getAlumnoAsignaturaDAO();

        Alumno alumno = alumnoDAO.getAlumnoByDNI(dni);

        JSONObject resultJson = new JSONObject();
        JSONObject documentJson = new JSONObject();
        documentJson.put("tipo", "notas-alumno-anyo");
        documentJson.put("formato", "format-02");
        documentJson.put("version", "1.0");
        resultJson.put("documento", documentJson);

        JSONObject alumnoJson = new JSONObject();
        alumnoJson.put("dni", alumno.getDni());
        alumnoJson.put("nombre", alumno.getNombre());
        alumnoJson.put("apellidos", alumno.getApellidos());
        resultJson.put("alumno", alumnoJson);

        resultJson.put("anyo", year);

        List<AlumnoAsignatura> asignaturas = alAsDAO.getAlumnoAsignaturaByAnyo(dni, year);
        JSONArray asignaturasArray = new JSONArray();
        for (AlumnoAsignatura asignatura : asignaturas) {
            JSONObject asignaturaJson = new JSONObject();
            asignaturaJson.put("codigo", asignatura.getAsignatura());
            asignaturaJson.put("nota", asignatura.getNota());
            asignaturasArray.put(asignaturaJson);
        }

        resultJson.put("asignaturas", asignaturasArray);
        return resultJson;
    }

    private static void closeConnection(Channel channel, Connection connection, String id) throws IOException, TimeoutException {
        String message = "close";
        channel.basicPublish(NOMBRE_EXCHANGE, TOPIC, null, message.getBytes());
        connection.close();
        System.out.println("\n[" + id + "] connection closed!");
    }
}