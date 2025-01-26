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

public class JSONConsumer implements Runnable{



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
            channel.queueBind(queueName, SOURCE_EXCHANGE, SOURCE_TOPIC_JSON);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println("[JSONConsumer] Received message: " + message);

                // Procesa el JSON recibido
                JSONObject receivedJson = new JSONObject(message);
                JSONObject responseJson = jsonToResponse(receivedJson);

                // Publicar el nuevo JSON en el exchange de destino
                String newMessage = responseJson.toString();
                channel.basicPublish(DESTINATION_EXCHANGE, "", null, newMessage.getBytes(StandardCharsets.UTF_8));
                System.out.println("[JSONConsumer] Published message to " + DESTINATION_EXCHANGE);
            };
            System.out.println("[JSONConsumer] running");

            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });

            // Keep the thread alive to keep consuming messages
            synchronized (this) {
                wait();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static JSONObject jsonToResponse(JSONObject receivedJson) {

        // Procesa el JSON recibido
        JSONObject alumno = receivedJson.getJSONObject("alumno");
        int year = receivedJson.getInt("anyo");
        JSONArray asignaturas = receivedJson.getJSONArray("asignaturas");

        //Calcula media de las notas
        double totalGrades = 0;
        double averageGrade = 0;
        if (asignaturas.length() > 0) {
            for (int i = 0; i < asignaturas.length(); i++) {
                totalGrades += asignaturas.getJSONObject(i).getDouble("nota");
            }
            averageGrade = totalGrades / asignaturas.length();
        }

        // crea el nuevo JSON respuesta
        JSONObject newJson = new JSONObject();
        newJson.put("alumno", alumno);
        newJson.put("anyo", year);
        newJson.put("asignaturas", asignaturas);
        newJson.put("nota-media", averageGrade);

//        File file = new File("NotasMiddleware" + File.separator + "ExpFinal_" + alumno + ".json");
//        file.getParentFile().mkdirs();
//        //Genera un archivo del expediente del alumno en formato CSV
//        try (FileWriter writer = new FileWriter(file)) {
//            String studentData = newJson.toString(4);
//            writer.write(studentData);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        System.out.println("[JSONConsumer] Transformed JSON to JSON: \n" + newJson.toString(4));

        return newJson;
    }

}