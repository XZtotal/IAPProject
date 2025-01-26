package iap.group1;

import com.rabbitmq.client.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

public class Visualizer {

    private static final String EXCHANGE_NAME = "notas.alumno.anyo";

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            // Declare the fanout exchange
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
            System.out.println("Declared a FANOUT exchange called " + EXCHANGE_NAME);

            // Declare a queue and bind it to the fanout exchange
            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, EXCHANGE_NAME, "");
            System.out.println("Bound queue " + queueName + " to exchange " + EXCHANGE_NAME);

            System.out.println("Esperando mensajes en la cola: " + queueName);
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println("Mensaje recibido: " + message);
            };
            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
                System.out.println("Cancelado: " + consumerTag);
            });

            // Keep the main thread alive
            CountDownLatch latch = new CountDownLatch(1);
            latch.await();

        } catch (IOException | TimeoutException e) {
            System.err.println("Error al conectar o procesar mensajes: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}