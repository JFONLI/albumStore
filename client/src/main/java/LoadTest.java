import okhttp3.*;
import java.io.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadTest {

    private static final int THREAD_GROUP_SIZE = 10;
    private static final int NUM_THREAD_GROUPS = 30;
    private static final String IPAddr = "AlbumStore-ALB-2028389129.us-west-2.elb.amazonaws.com";
    private static final int DELAY = 2000; // milliseconds
    private static final int REQUESTS_PER_THREADS = 1000;
    private static final int MAX_RETRY = 5;
    private static AtomicInteger getSuccess = new AtomicInteger(0);
    private static AtomicInteger postSuccess = new AtomicInteger(0);
    private static final File image = new File("image_example.jpg");

    private static OkHttpClient client = new OkHttpClient.Builder()
            .connectionPool(new ConnectionPool(30, 1, TimeUnit.MINUTES)) // 設置連接池
            .build();

    private static List<PerformanceRecord> performanceRecords = Collections.synchronizedList(new ArrayList<>());

    private static <T> void writeListToCSV(List<T> dataList, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            // 檢查列表是否為空
            if (dataList.isEmpty()) {
                System.out.println("列表為空，無法生成 CSV 文件。");
                return;
            }

            // 獲取第一個元素的類型
            Class<?> clazz = dataList.get(0).getClass();

            // 獲取類型的所有字段
            Field[] fields = clazz.getDeclaredFields();

            // 寫入 CSV 文件的標題行
            for (int i = 0; i < fields.length; i++) {
                writer.write(fields[i].getName());
                if (i < fields.length - 1) {
                    writer.write(",");
                } else {
                    writer.write("\n");
                }
            }

            // 將每個元素寫入到 CSV 文件中
            for (T data : dataList) {
                for (int i = 0; i < fields.length; i++) {
                    fields[i].setAccessible(true);
                    Object value = fields[i].get(data);
                    writer.write(value.toString());
                    if (i < fields.length - 1) {
                        writer.write(",");
                    } else {
                        writer.write("\n");
                    }
                }
            }

            System.out.println("CSV 文件成功生成：" + filePath);
        } catch (IOException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static void sortByEndTime(List<PerformanceRecord> records) {
        // 使用 Comparator 匿名類來定義比較規則
        Collections.sort(records, new Comparator<PerformanceRecord>() {
            @Override
            public int compare(PerformanceRecord record1, PerformanceRecord record2) {
                return Long.compare(record1.getEndTime(), record2.getEndTime());
            }
        });
    }
    private static void calculateThroughput() {
        sortByEndTime(performanceRecords);

        List<ThroughputDataPoint> throughputData = new ArrayList<>();
        long startTime = performanceRecords.get(0).getEndTime();
        long endTime = performanceRecords.get(performanceRecords.size() - 1).getEndTime();
        int currentSecond = 0;
        int requestsInCurrentSecond = 0;

        for (PerformanceRecord record : performanceRecords) {
            long timestamp = record.getEndTime();
            int requestCount = 1;

            if ((timestamp - startTime) / 1000 == currentSecond) {
                requestsInCurrentSecond += requestCount;
            } else {

                throughputData.add(new ThroughputDataPoint(currentSecond, requestsInCurrentSecond));
                currentSecond = (int) ((timestamp - startTime) / 1000);
                requestsInCurrentSecond = requestCount;
            }
        }

        throughputData.add(new ThroughputDataPoint(currentSecond, requestsInCurrentSecond));

        writeListToCSV( throughputData, "throughputdata.csv");

    }


    public static void main(String[] args) throws InterruptedException, IOException {

        ExecutorService executor = Executors.newCachedThreadPool();

        long mainStartTime = System.currentTimeMillis();
        for(int i=0; i<NUM_THREAD_GROUPS; i++){
            System.out.println("Start " + (i+1) + " Group");
            for(int j=0; j<THREAD_GROUP_SIZE; j++){
                executor.submit(() -> {
                  threadTask();
                });
            }

            Thread.sleep(DELAY);
        }

        shutdownAndAwaitTermination(executor);
//        executor.shutdown();
//        executor.awaitTermination(1, TimeUnit.HOURS);

        long mainEndTime = System.currentTimeMillis();
        System.out.println("All tasks completed");
        System.out.println("Wall Time : " + (double)(mainEndTime - mainStartTime)/1000 + " seconds.");
        System.out.println("Success GET requests : " + getSuccess.intValue() + ".");
        System.out.println("Success POST requests : " + postSuccess.intValue() + ".");

        String csvFilePath = "performance.csv";
        writeListToCSV(performanceRecords, csvFilePath);
        calculateThroughput();

        System.out.println("Done");

    }

    private static void threadTask() {
        for(int i=0; i<REQUESTS_PER_THREADS; i++){

            long postStartTime = System.currentTimeMillis();
            for(int k=0; k<MAX_RETRY; k++){
                Response postResult = sendPostRequest();
                if(postResult != null){
                    if(postResult.code() == 200){
                        long postEndTime = System.currentTimeMillis();
                        long postLatency = postEndTime - postStartTime;
                        System.out.println("POST DONE");
                        performanceRecords.add(new PerformanceRecord(postStartTime, postEndTime, "POST", postLatency, postResult.code()));
                        postSuccess.incrementAndGet();
                        postResult.close();
                        break;
                    }
                    else{
                        postResult.close();
                    }
                }
                if(postResult != null)   postResult.close();
                System.out.println("Retrying POST request with " + (k+1) + " time");
                waitSeconds(5);
            }

            long getStartTime = System.currentTimeMillis();
            for(int j=0; j<MAX_RETRY; j++){
                Response getResult = sendGetRequest();
                if(getResult != null){
                    if(getResult.code() == 200){
                        long getEndTime = System.currentTimeMillis();
                        long getLatency = getEndTime - getStartTime;
                        System.out.println("GET  DONE");
                        performanceRecords.add(new PerformanceRecord(getStartTime, getEndTime, "GET", getLatency, getResult.code()));
                        getSuccess.incrementAndGet();
                        break;
                    } else{
                        getResult.close();
                    }
                }
                if(getResult != null)   getResult.close();
                System.out.println("Retrying GET request with " + (j+1) + " time");
                waitSeconds(5);
            }

        }
    }

    private static void waitSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static Response sendPostRequest() {
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart(
                        "image",
                        "image_example.jpg",
                        RequestBody.create(MediaType.parse("image/jpeg"),
                                image)
                )
                .addFormDataPart("profile", "{ \"artist\": \"Jimmy\", \"title\": \"NEU\", \"year\": \"0153\"}")
                .build();

        Request request = new Request.Builder()
                //.url("http://" + IPAddr + ":8080/albumStore_war_exploded/albums")
                .url("http://" + IPAddr + ":8080/albumStore/albums")
                //.url("http://35.163.31.108:8080/albumStore_war_exploded/albums")
                .method("POST", body)
                .build();
        try (Response response = client.newCall(request).execute()){
            // System.out.println("POST Status code : " + response.code() + " ");
            return response;
        } catch (IOException e){
            System.out.println("POST error : " + e);
            return null;
        }
    }

    private static Response sendGetRequest() {
        String albumId = "1";
        Request request = new Request.Builder()
                //.url("http://" + IPAddr + ":8080/albumStore_war_exploded/albums/" + albumId)
                .url("http://" + IPAddr + ":8080/albumStore/albums/" + albumId)
                //.url("http://35.163.31.108:8080/albumStore/albums/8f368251-77f5-4701-a899-567a1a7a26e6")
                .method("GET", null)
                .build();
        try (Response response = client.newCall(request).execute()) {
            // Response response = client.newCall(request).execute();
            // System.out.println("GET  Status code : " + response.code() + " ");
            // System.out.println(response.body().string());
            return response;
        } catch(IOException e){
            System.out.println("GET  error : " + e.getMessage());
            return null;
        }
    }
    private static class PerformanceRecord{
        private long startTime;
        private long endTime;
        private String requestType;
        private long latency;
        private int responseCode;

        public PerformanceRecord(long startTime, long endTime, String requestType, long latency, int responseCode){
            this.startTime = startTime;
            this.endTime = endTime;
            this.requestType = requestType;
            this.latency = latency;
            this.responseCode = responseCode;
        }

        public long getStartTime(){
            return startTime;
        }
        public long getEndTime(){
            return endTime;
        }

        public String getRequestType(){
            return requestType;
        }

        public long getLatency(){
            return latency;
        }

        public int getResponseCode(){
            return responseCode;
        }

    }

        private static class ThroughputDataPoint {
        private int second;
        private int requestCount;

        public ThroughputDataPoint(int second, int requestCount) {
            this.second = second;
            this.requestCount = requestCount;
        }

        public int getSecond() {
            return second;
        }

        public int getRequestCount() {
            return requestCount;
        }
    }

    private static void shutdownAndAwaitTermination(ExecutorService executor) {
        executor.shutdown();

        try {
            if (!executor.awaitTermination(1L, TimeUnit.HOURS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException var2) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

    }




}
