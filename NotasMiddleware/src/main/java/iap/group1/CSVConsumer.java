package iap.group1;

import com.rabbitmq.client.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class CSVConsumer implements Runnable {

    private static final String SOURCE_EXCHANGE = "primer-exchange";
    private static final String SOURCE_TOPIC = "generador.csv";
    private static final String DESTINATION_EXCHANGE = "notas.alumnos.anyo";

    public void run() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");
        System.out.println("CSVConsumer running");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(SOURCE_EXCHANGE, BuiltinExchangeType.TOPIC);
            channel.exchangeDeclare(DESTINATION_EXCHANGE, BuiltinExchangeType.FANOUT);

            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, SOURCE_EXCHANGE, SOURCE_TOPIC);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println("Received message: " + message);


                // Publicar el nuevo JSON en el exchange de destino
                String newMessage = transformCsvToCanonical(message).toString();
                channel.basicPublish(DESTINATION_EXCHANGE, "", null, newMessage.getBytes(StandardCharsets.UTF_8));
                System.out.println("Published message to " + DESTINATION_EXCHANGE);
            };

            try {
                System.out.println("CSVConsumer running");

                channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });
                // Keep the thread alive to keep consuming messages
                synchronized (this) {
                    wait();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private static JSONObject transformCsvToCanonical(String message) {
        // Procesa el CSV recibido
        String[] lines = message.split("\n");
        String[] alumnoData = lines[2].split(",");
        String dni = alumnoData[0].trim();
        String nombre = alumnoData[1].trim();
        String apellidos = alumnoData[2].trim();
        int year = Integer.parseInt(lines[3].trim());

        // Procesa las asignaturas y calcula la nota media
        double totalGrades = 0;
        int numAsignaturas = 0;
        JSONArray asignaturas = new JSONArray();
        for (int i = 4; i < lines.length; i++) {
            String[] asignaturaData = lines[i].split(",");
            String codigo = asignaturaData[0].trim();
            double nota = Double.parseDouble(asignaturaData[1].trim());
            totalGrades += nota;
            numAsignaturas++;

            JSONObject asignatura = new JSONObject();
            asignatura.put("codigo", codigo);
            asignatura.put("nota", nota);
            asignaturas.put(asignatura);
        }
        double averageGrade = totalGrades / numAsignaturas;

        // Crea el nuevo JSON respuesta
        JSONObject newJson = new JSONObject();
        JSONObject alumno = new JSONObject();
        alumno.put("dni", dni);
        alumno.put("nombre", nombre);
        alumno.put("apellidos", apellidos);
        newJson.put("alumno", alumno);
        newJson.put("anyo", year);
        newJson.put("asignaturas", asignaturas);
        newJson.put("nota-media", averageGrade);
        return newJson;

    }
}