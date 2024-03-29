package servlet;

import java.util.Date;
import java.text.SimpleDateFormat;

import com.google.gson.Gson;
import com.rabbitmq.client.DeliverCallback;
import dao.AlbumInfoDao;
import model.AlbumInfo;
import model.ErrorMsg;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.nio.Buffer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import dto.Album;
import model.ImageMetaData;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

@WebServlet(name = "Servlet", value = "/Servlet")
@MultipartConfig
public class albumStoreServlet extends HttpServlet {
    private ExecutorService executorService = Executors.newFixedThreadPool(5);
    private BlockingQueue<Album> blockingQueue = new LinkedBlockingQueue(100000);

    private Connection connection;
    private final static String QUEUE_NAME = "like_dislike_queue";

    private Gson gson = new Gson();
    private byte[] imageContent = new byte[30000];

//    public void init(ServletConfig config) throws ServletException {
//        super.init(config);
//        try {
//            ConnectionFactory factory = new ConnectionFactory();
//            factory.setHost("localhost");
//            connection = factory.newConnection();
//            startConsumer();
//        } catch (IOException | TimeoutException e){
//            e.printStackTrace();
//        }
//    }

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
                        System.out.println("Doing work");
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

//    private class BackgroundService implements Runnable {
//        private List<Album> temp = new ArrayList<>();
//        @Override
//        public void run() {
//            while (true) {
//                try {
//                    if(temp.size() >= 50){
//                        AlbumInfoDao albumInfoDao = new AlbumInfoDao();
//                        System.out.println("Start writing to DB");
//                        albumInfoDao.createAlbumBatch(temp);
//                        temp.clear();
//                    } else{
//                        temp.add(blockingQueue.take());
//                    }
//                    System.out.println("Queue Size : " + blockingQueue.size());
//                } catch (InterruptedException e) {
//                    System.out.println("BackgroundService interrupted");
//                    Thread.currentThread().interrupt();
//                } catch (SQLException e) {
//                    System.out.println("Error writing data to DB");
//                }
//            }
//        }
//    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
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
        if(urlParts.length != 2){
            ErrorMsg err = new ErrorMsg();
            err.setMsg("invalid url request parameters");
            String jsonErr = gson.toJson(err);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(jsonErr);
            return;
        }

        String key = urlParts[1];
        AlbumInfoDao albumInfoDao = new AlbumInfoDao();

        Album album = albumInfoDao.findByAlbumId(Integer.parseInt(key));
        if(album == null){
            ErrorMsg err = new ErrorMsg();
            err.setMsg("Key not found");
            String jsonErr = gson.toJson(err);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write(jsonErr);
            return;
        }

        AlbumInfo albumInfo = gson.fromJson(album.getInfo(), AlbumInfo.class);
        String jsonAlbumInfo = gson.toJson(albumInfo);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(jsonAlbumInfo);

//        // Testing
//        AlbumInfo albumInfo = new AlbumInfo("Jimmy", "Testing dummy", "123");
//        String jsonAlbumInfo = gson.toJson(albumInfo);
//        response.setStatus(HttpServletResponse.SC_OK);
//        response.getWriter().write(jsonAlbumInfo);

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Get requests parts
        Part imagePart = request.getPart("image");
        Part profilePart = request.getPart("profile");

        // Missing parts
        if(imagePart == null){
            ErrorMsg err = new ErrorMsg();
            err.setMsg("Missing image");
            String jsonErr = gson.toJson(err);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(jsonErr);
            return;
        }

        if(profilePart == null){
            ErrorMsg err = new ErrorMsg();
            err.setMsg("Missing profile");
            String jsonErr = gson.toJson(err);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(jsonErr);
            return;
        }

        StringBuilder builder = null;

        try (InputStream profileInputStream = profilePart.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(profileInputStream))){
            builder = new StringBuilder();
            String line;
            while((line = reader.readLine())!=null){
                builder.append(line);
                builder.append("\n");
            }
        }


        AlbumInfo albumInfo;
        try{
            albumInfo = (AlbumInfo) gson.fromJson(builder.toString(), AlbumInfo.class);
        } catch (Exception e){
            ErrorMsg err = new ErrorMsg();
            err.setMsg("Error with profile json");
            String jsonErr = gson.toJson(err);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(jsonErr);
            return;
        }

        String imageBase64;
        // Deal with imagePart
//        try (BufferedReader imageBufferedReader = new BufferedReader(new InputStreamReader(imagePart.getInputStream()))){
//            StringBuilder imageBuilder = new StringBuilder();
//            String imageLine;
//            while((imageLine = imageBufferedReader.readLine()) != null){
//                imageBuilder.append(imageLine);
//            }
//            imageContent = imageBuilder.toString().getBytes();
//            imageBase64 = Base64.getEncoder().encodeToString(imageContent);
//        }

        try (InputStream inputStream = imagePart.getInputStream()) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024]; // 使用缓冲区
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            imageContent = outputStream.toByteArray();
            imageBase64 = Base64.getEncoder().encodeToString(imageContent);
        }

//         Store Data to DB
        Album album = new Album(1, imageBase64, gson.toJson(albumInfo));
        try {
            AlbumInfoDao albumInfoDao =  new AlbumInfoDao();
            int albumId = albumInfoDao.createAlbum(album);
            ImageMetaData imageData = new ImageMetaData();
            imageData.setImageSize(String.valueOf(imagePart.getSize()));
            imageData.setAlbumID(String.valueOf(albumId));
            String jsonImageData = gson.toJson(imageData);


            response.getWriter().write(jsonImageData);
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e){
            e.printStackTrace();
        }

    }

}