package servlet;

import com.google.gson.Gson;
import dao.AlbumInfoDao;
import model.AlbumInfo;
import model.ErrorMsg;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dto.Album;
import model.ImageMetaData;
import org.json.JSONObject;

@WebServlet(name = "Servlet", value = "/Servlet")
@MultipartConfig
public class albumStoreServlet extends HttpServlet {
    private List<Album> albums = Collections.synchronizedList(new ArrayList<>());
    private AtomicInteger batchCounter = new AtomicInteger(0);
    private ExecutorService executorService = Executors.newFixedThreadPool(30);
    private BlockingQueue<Album> blockingQueue = new LinkedBlockingQueue();

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        BackgroundService backgroundService = new BackgroundService();
        executorService.submit(backgroundService);
    }

    private class BackgroundService implements Runnable {
        private List<Album> temp = new ArrayList<>();
        @Override
        public void run() {
            while (true) {
                try {
                    if(temp.size() >= 200){
                        AlbumInfoDao albumInfoDao = new AlbumInfoDao();
                        System.out.println("Start writing to DB");
                        albumInfoDao.createAlbum(temp);
                        temp.clear();
                    } else{
                        temp.add(blockingQueue.take());
                    }
                    System.out.println("Queue Size : " + blockingQueue.size());
                } catch (InterruptedException e) {
                    System.out.println("BackgroundService interrupted");
                    Thread.currentThread().interrupt();
                } catch (SQLException e) {
                    System.out.println("Error writing data to DB");
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Gson gson = new Gson();

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

        // Gson
        Gson gson = new Gson();

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

        // Deal with profile part
        InputStream profileInputStream = profilePart.getInputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(profileInputStream));
        StringBuilder builder = new StringBuilder();
        String line;
        while((line = reader.readLine())!=null){
            builder.append(line);
            builder.append("\n");
        }

        // System.out.println(builder.toString());

//        Pattern pattern = Pattern.compile("\\{([^}]*)\\}");
//        Matcher matcher = pattern.matcher(builder.toString());
//
//        JSONObject jsonObject = new JSONObject();
//        if (matcher.find()) {
//            String jsonContent = matcher.group(1);
//
//            String[] keyValuePairs = jsonContent.split("\n");
//            for (String pair : keyValuePairs) {
//                String[] keyValue = pair.trim().split(":");
//                if (keyValue.length == 2) {
//                    String key = keyValue[0].trim();
//                    String value = keyValue[1].trim();
//                    jsonObject.put(key, value);
//                }
//            }
//
//            System.out.println(jsonObject.toString());
//        } else {
//            System.out.println("Cannot find Content");
//        }

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

        // Deal with imagePart
        BufferedReader imageBufferedReader = new BufferedReader(new InputStreamReader(imagePart.getInputStream()));
        StringBuilder imageBuilder = new StringBuilder();
        String imageLine;
        while((imageLine = imageBufferedReader.readLine()) != null){
            imageBuilder.append(imageLine);
        }
        byte[] imageContent = imageBuilder.toString().getBytes();
        String imageBase64 = Base64.getEncoder().encodeToString(imageContent);


        // Store Data to DB
         UUID albumId = UUID.randomUUID();
         Album album = new Album(1, imageBase64, gson.toJson(albumInfo));
        try {
            blockingQueue.put(album);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        ImageMetaData imageData = new ImageMetaData();
        imageData.setImageSize(String.valueOf(imagePart.getSize()));
        imageData.setAlbumID("1");
        String jsonImageData = gson.toJson(imageData);

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(jsonImageData);
//        } catch(SQLException e) {
//            ErrorMsg err = new ErrorMsg();
//            err.setMsg("Database error: " + "Insert Data into DB " +  System.getProperty("MySQL_IP_ADDRESS"));
//            String jsonErr = gson.toJson(err);
//            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//            response.getWriter().write(jsonErr);
//        }

//        // Testing
//        ImageMetaData imageData = new ImageMetaData();
//        // imageData.setImageSize(String.valueOf(imagePart.getSize()));
//        String jsonImageData = gson.toJson(imageData);
//
//        response.setStatus(HttpServletResponse.SC_CREATED);
//        response.getWriter().write(jsonImageData);

    }

}