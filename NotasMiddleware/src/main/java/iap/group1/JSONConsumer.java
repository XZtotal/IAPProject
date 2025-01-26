package iap.group1;

import com.rabbitmq.client.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class JSONConsumer implements Runnable{

    private static final String SOURCE_EXCHANGE = "primer-exchange";
    private static final String SOURCE_TOPIC = "generador.json";
    private static final String DESTINATION_EXCHANGE = "notas.alumnos.anyo";

    public void run() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(SOURCE_EXCHANGE, BuiltinExchangeType.TOPIC);
            channel.exchangeDeclare(DESTINATION_EXCHANGE, BuiltinExchangeType.FANOUT);

            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, SOURCE_EXCHANGE, SOURCE_TOPIC);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println("Received message: " + message);

                // Procesa el JSON recibido
                JSONObject receivedJson = new JSONObject(message);
                JSONObject responseJson = jsonToResponse(receivedJson);

                // Publicar el nuevo JSON en el exchange de destino
                String newMessage = responseJson.toString();
                channel.basicPublish(DESTINATION_EXCHANGE, "", null, newMessage.getBytes(StandardCharsets.UTF_8));
                System.out.println("Published message to " + DESTINATION_EXCHANGE);
            };

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
        for (int i = 0; i < asignaturas.length(); i++) {
            totalGrades += asignaturas.getJSONObject(i).getDouble("nota");
        }
        double averageGrade = totalGrades / asignaturas.length();

        // crea el nuevo JSON respuesta
        JSONObject newJson = new JSONObject();
        newJson.put("alumno", alumno);
        newJson.put("anyo", year);
        newJson.put("asignaturas", asignaturas);
        newJson.put("nota-media", averageGrade);

        return newJson;
    }

}