package servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import dao.ReviewDao;
import model.ErrorMsg;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

@WebServlet(name = "Servlet", value = "/Servlet")
public class reviewServlet extends HttpServlet {
    private final static String QUEUE_NAME = "like_dislike_queue";
    private Connection connection;
    private Gson gson = new Gson();

    private GenericObjectPool<Channel> channelPool;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            connection = factory.newConnection();

            GenericObjectPoolConfig<Channel> poolConfig = new GenericObjectPoolConfig<>();
            poolConfig.setMaxTotal(10);
            poolConfig.setMinIdle(5);
            channelPool = new GenericObjectPool<>(new ChannelFactory(connection), poolConfig);

            //startConsumer();
        } catch (IOException | TimeoutException e){
            e.printStackTrace();
        }
    }
    private void startConsumer() {
        Thread consumerThread = new Thread(() -> {
            try {

                Channel channel = connection.createChannel();
                channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                channel.basicQos(1);
                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), "UTF-8");

                    String[] parts = message.split(":");
                    String action = parts[0];
                    int albumId = Integer.parseInt(parts[1]);

                    if(action.equals("like")) {
                        ReviewDao likedao = new ReviewDao();
                        likedao.updateAlbumLikes(albumId, 1);
                    } else if (action.equals("dislike")){
                        ReviewDao likedao = new ReviewDao();
                        likedao.updateAlbumDislikes(albumId, 1);
                    }
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                };

                // process messages
                channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> { });

            } catch(IOException e){
                e.printStackTrace();
            }
        });

        consumerThread.start();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String urlPath = request.getPathInfo();
        if(urlPath == null){
            ErrorMsg err = new ErrorMsg();
            err.setMsg("missing url");
            String jsonErr = gson.toJson(err);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(jsonErr);
            return;
        }

        String[] urlParts = urlPath.split("/");
        if(urlParts.length != 3){
            ErrorMsg err = new ErrorMsg();
            err.setMsg("invalid url request parameters");
            String jsonErr = gson.toJson(err);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(jsonErr);
            return;
        }

        String message = urlParts[1];

        if (!message.equals("like") && !message.equals("dislike")) {
            ErrorMsg err = new ErrorMsg();
            err.setMsg("Invalid inputs");
            String jsonErr = gson.toJson(err);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(jsonErr);
            return;
        }

        String key = urlParts[2];
        int albumId = Integer.parseInt(key);

        try  {
            Channel channel = getChannelFromPool();
            String routingKey = message.equals("like") ? "like" : "dislike";
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            channel.basicPublish("", QUEUE_NAME, null, (routingKey + ":" + albumId).getBytes("UTF-8"));
            returnChannelToPool(channel);
        } catch (Exception e) {
            e.printStackTrace();
            ErrorMsg err = new ErrorMsg();
            err.setMsg("Failed to send message to RabbitMQ");
            String jsonErr = gson.toJson(err);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(jsonErr);
            return;
        }

        response.setStatus(HttpServletResponse.SC_CREATED);
        response.setContentType("text/plain");
        response.getWriter().println("Write Successful");

    }

    private class ChannelFactory extends BasePooledObjectFactory<Channel>{
        private Connection connection;

        public ChannelFactory(Connection connection){
            this.connection = connection;
        }
        @Override
        public Channel create() throws Exception{
            return connection.createChannel();
        }
        @Override
        public PooledObject<Channel> wrap(Channel obj){
            return new DefaultPooledObject<>(obj);
        }
        @Override
        public void destroyObject(PooledObject<Channel> p) throws Exception{
            p.getObject().close();
        }
    }

    private Channel getChannelFromPool() throws Exception {
        return channelPool.borrowObject();
    }

    private void returnChannelToPool(Channel channel) {
        try {
            channelPool.returnObject(channel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}