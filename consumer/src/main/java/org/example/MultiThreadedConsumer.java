package org.example;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import dao.ReviewDao;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class MultiThreadedConsumer implements Runnable {
    private final ConnectionFactory factory;
    private final String queueName;

    public MultiThreadedConsumer(ConnectionFactory factory, String queueName) {
        this.factory = factory;
        this.queueName = queueName;
    }

    @Override
    public void run() {
        try {
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(queueName, false, false, false, null);
            System.out.println("Waiting for messages...");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                 System.out.println("Received message: " + message);

                String[] parts = message.split(":");
                int albumId = Integer.parseInt(parts[1]);
                ReviewDao reviewDao = new ReviewDao();
                if(parts[0].equals("like")) {
                    reviewDao.updateAlbumLikes(albumId, 1);
                } else{
                    reviewDao.updateAlbumDislikes(albumId, 1);
                }
//                 // channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

            };

            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
//        factory.setHost("18.237.85.19");
//        factory.setUsername("guest1");
//        factory.setPassword("guest1");

        String host = args[0];
        String username = args[1];
        String password = args[2];

        factory.setHost(host);
        factory.setUsername(username);
        factory.setPassword(password);

        // 启动多个消费者实例，每个实例运行在一个单独的线程中
        for (int i = 0; i < 5; i++) {
            Thread thread = new Thread(new MultiThreadedConsumer(factory, "like_dislike_queue"));
            thread.start();
        }
    }
}
