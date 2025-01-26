package iap.group1;

import com.rabbitmq.client.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import static iap.group1.NotasMiddleware.*;

public class CSVConsumer implements Runnable {




    public void run() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST);
        factory.setPort(PORT);
        factory.setUsername(USERNAME);
        factory.setPassword(PASSWORD);
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(SOURCE_EXCHANGE, BuiltinExchangeType.TOPIC);
            channel.exchangeDeclare(DESTINATION_EXCHANGE, BuiltinExchangeType.FANOUT);

            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, SOURCE_EXCHANGE, SOURCE_TOPIC_CSV);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println("[CSVConsumer] Received message: " + message);



                // Publicar el nuevo JSON en el exchange de destino
                String newMessage = transformCsvToCanonical(message).toString();



                channel.basicPublish(DESTINATION_EXCHANGE, "", null, newMessage.getBytes(StandardCharsets.UTF_8));
                System.out.println("[CSVConsumer] Published message to " + DESTINATION_EXCHANGE);
            };

            try {
                System.out.println("[CSVConsumer] running");


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
        double averageGrade = 0;
        if (numAsignaturas > 0) {
            averageGrade = totalGrades / numAsignaturas;
        }

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

//        File file = new File("NotasMiddleware" + File.separator + "ExpFinal_" + dni + ".json");
//        file.getParentFile().mkdirs();
//
//        //Genera un archivo del expediente del alumno en formato CSV
//        try (FileWriter writer = new FileWriter(file)) {
//            String studentData = newJson.toString(4);
//            writer.write(studentData);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        System.out.println("[CSVConsumer] Transformed CSV to JSON: \n" + newJson.toString(4));

        return newJson;

    }
}