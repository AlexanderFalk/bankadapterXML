import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Publish {
    public static void publishQueue(String message) {
        try {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("5.179.80.218");
        Connection connection = factory.newConnection();

        Channel replyChannel = connection.createChannel();
        String replyQueue = "Group14_Bank_Response_Queue";
        replyChannel.queueDeclare(replyQueue , false, false, false, null);
        String replyKey = "Group14_Bank_Response_Queue";
        // Send request XMLmessage
        AMQP.BasicProperties basicProperties = new AMQP.BasicProperties()
                .builder()
                .contentType("text/plain")
                .deliveryMode(1)
                .replyTo(replyKey)
                .build();
        replyChannel.basicPublish("", replyKey, null, message.getBytes());

        System.out.println("Message is send to queue: " + replyKey);
        replyChannel.close();
        connection.close();

        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
