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
import dao.LikeDao;
import model.ErrorMsg;

@WebServlet(name = "Servlet", value = "/Servlet")
public class reviewServlet extends HttpServlet {
    private final static String QUEUE_NAME = "like_dislike_queue";
    private Connection connection;
    private Gson gson = new Gson();

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            connection = factory.newConnection();
            startConsumer();
        } catch (IOException | TimeoutException e){
            e.printStackTrace();
        }
    }
    private void startConsumer() {
        Thread consumerThread = new Thread(() -> {
            try {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost("localhost");
                final Connection connection = factory.newConnection();
                final Channel channel = connection.createChannel();
                channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                channel.basicQos(1);
                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), "UTF-8");

                    System.out.println(" [x] Received '" + message + "'");
                    try {
                        doWork();
                    } finally {
                        System.out.println(" [x] Done");
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    }
                };

                // process messages
                channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> { });

            } catch(IOException | TimeoutException e){
                e.printStackTrace();
            }
        });

        consumerThread.start();
    }

    private static void doWork(){

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

        LikeDao likeDao = new LikeDao();
        int rowsAffected = 0;
        if(message.equals("like")){
            rowsAffected = likeDao.updateAlbumLikes(albumId, 1);
        }
        else{
            rowsAffected = likeDao.updateAlbumDislikes(albumId, 1);
        }
        if(rowsAffected == 0){
            ErrorMsg err = new ErrorMsg();
            err.setMsg("Album not found");
            String jsonErr = gson.toJson(err);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write(jsonErr);
            return;
        }


        response.setStatus(HttpServletResponse.SC_CREATED);
        response.setContentType("text/plain");
        response.getWriter().println("Write Successful");


    }
}