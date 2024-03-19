import io.swagger.client.*;
import io.swagger.client.auth.*;
import io.swagger.client.model.*;
import io.swagger.client.api.DefaultApi;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class LoadTestProgramPart2 {
    private static int threadGroupSize;
    private static int numThreadGroups;
    private static int delay;
    private static String IPAddr;

    private static List<PerformanceRecord> performanceRecords = Collections.synchronizedList(new ArrayList<>());

    private static List<ThroughputRecord> throughputRecords = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws InterruptedException {
//
//
        // Step 1
        threadGroupSize = 10;
        numThreadGroups = 10;
        delay = 2;
        // AWS - Tomcat - DummyServer
        // IPAddr =  "http://54.185.63.202:8080/hw4";
        // AWS - Tomcat - TrueServer
        //IPAddr =  "http://54.148.3.22:8080/albumStore";
        //IPAddr =  "http://34.220.113.165:8080";

        runLoadTest();

    }

    public static void exportToCSV(String filePath, List<ThroughputDataPoint> throughputData) {
        try (FileWriter writer = new FileWriter(filePath)) {

            writer.append("Time (seconds),Throughput (requests/second)\n");

            for (ThroughputDataPoint dataPoint : throughputData) {
                writer.append(dataPoint.getSecond() + "," + dataPoint.getRequestCount() + "\n");
            }

            System.out.println("CSV file exported successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void calculateThroughput() {

        List<ThroughputDataPoint> throughputData = new ArrayList<>();
        long startTime = throughputRecords.get(0).getTimestamp();
        long endTime = throughputRecords.get(throughputRecords.size() - 1).getTimestamp();
        int currentSecond = 0;
        int requestsInCurrentSecond = 0;

        for (ThroughputRecord record : throughputRecords) {
            long timestamp = record.getTimestamp();
            int requestCount = record.getRequestCount();

            if ((timestamp - startTime) / 1000 == currentSecond) {
                requestsInCurrentSecond += requestCount;
            } else {

                throughputData.add(new ThroughputDataPoint(currentSecond, requestsInCurrentSecond));
                currentSecond = (int) ((timestamp - startTime) / 1000);
                requestsInCurrentSecond = requestCount;
            }
        }

        throughputData.add(new ThroughputDataPoint(currentSecond, requestsInCurrentSecond));

        exportToCSV("throughputdata.csv", throughputData);

    }

    private static void calculateAndDisplayStatistics() {
        List<Long> postLatencies = new ArrayList<>();
        List<Long> getLatencies = new ArrayList<>();
        int postSuccessCount = 0;
        int getSuccessCount = 0;

        for (PerformanceRecord record : performanceRecords) {
            if (record.getRequestType().equals("POST")) {
                postLatencies.add(record.getLatency());
                if (record.getResponseCode() >= 200 && record.getResponseCode() < 300) {
                    postSuccessCount++;
                }
            } else if (record.getRequestType().equals("GET")) {
                getLatencies.add(record.getLatency());
                if (record.getResponseCode() >= 200 && record.getResponseCode() < 300) {
                    getSuccessCount++;
                }
            }
        }

        // 计算POST请求的统计指标
        System.out.println("POST Statistics:");
        System.out.println("Total POST Requests: " + postLatencies.size());
        System.out.println("Mean Response Time: " + calculateMean(postLatencies) + " milliseconds");
        System.out.println("Median Response Time: " + calculateMedian(postLatencies) + " milliseconds");
        System.out.println("P99 Response Time: " + calculatePercentile(postLatencies, 99) + " milliseconds");
        System.out.println("Min Response Time: " + Collections.min(postLatencies) + " milliseconds");
        System.out.println("Max Response Time: " + Collections.max(postLatencies) + " milliseconds");
        System.out.println("POST Success Rate: " + ((double) postSuccessCount / postLatencies.size()) * 100 + "%");

        // 计算GET请求的统计指标
        System.out.println("\nGET Statistics:");
        System.out.println("Total GET Requests: " + getLatencies.size());
        System.out.println("Mean Response Time: " + calculateMean(getLatencies) + " milliseconds");
        System.out.println("Median Response Time: " + calculateMedian(getLatencies) + " milliseconds");
        System.out.println("P99 Response Time: " + calculatePercentile(getLatencies, 99) + " milliseconds");
        System.out.println("Min Response Time: " + Collections.min(getLatencies) + " milliseconds");
        System.out.println("Max Response Time: " + Collections.max(getLatencies) + " milliseconds");
        System.out.println("GET Success Rate: " + ((double) getSuccessCount / getLatencies.size()) * 100 + "%");
    }

    private static long calculateMean(List<Long> latencies) {
        long sum = 0;
        for (long latency : latencies) {
            sum += latency;
        }
        return latencies.isEmpty() ? 0 : sum / latencies.size();
    }

    private static long calculateMedian(List<Long> latencies) {
        Collections.sort(latencies);
        int size = latencies.size();
        return size % 2 == 0 ?
                (latencies.get(size / 2 - 1) + latencies.get(size / 2)) / 2 :
                latencies.get(size / 2);
    }

    private static long calculatePercentile(List<Long> latencies, int percentile) {
        Collections.sort(latencies);
        int index = (int) Math.ceil(latencies.size() * (percentile / 100.0)) - 1;
        return latencies.get(index);
    }


    private static void runLoadTest() throws InterruptedException {
        System.out.println("Start testing....");
        System.out.println("hardcoded initialization completed");
        System.out.println("Start main testing");
        threads.clear();
        // Step 3
        long startTime = System.currentTimeMillis();

        // Step 4
        for (int i = 0; i < numThreadGroups; i++) {
            System.out.println("Start " + i + " Group");
            initializeThreads(threadGroupSize, 1000);
            if (i < numThreadGroups - 1) {
                waitSeconds(delay);
            }
        }


        for (Thread thread : threads) {
            thread.join();
        }

        long endTime = System.currentTimeMillis();

        calculateAndDisplayStatistics();
        calculateThroughput();
        printResults(startTime, endTime);
    }
    private static List<Thread> threads = new ArrayList<>();
    private static void initializeThreads(int numThreads, int numIterations) throws InterruptedException {


        for (int i = 0; i < numThreads; i++) {
            Thread thread = new Thread(() -> {
                for (int j = 0; j < numIterations; j++) {
                    makePostRequest();
                    makeGetRequest();
                }
            });
            threads.add(thread);
            thread.start();
        }
    }

    private static void makePostRequest() {
        long startTime = System.currentTimeMillis();
        DefaultApi apiInstance = new DefaultApi();
        apiInstance.getApiClient().setBasePath(IPAddr);
        File image = new File("image_example.jpg"); // File |
        AlbumsProfile profile = new AlbumsProfile(); // AlbumsProfile |
        profile.setArtist("Jerry");
        profile.setTitle("Hello");
        profile.setYear("1998");

        // retry request for 5 times
        for (int i = 0; i < 5; i++) {
            try {
                ApiResponse<ImageMetaData> result = apiInstance.newAlbum(image, profile);
                // ImageMetaData result = apiInstance.newAlbum(image, profile);
                long endTime = System.currentTimeMillis();
                long latency = endTime - startTime;
                System.out.println("POST success");

                performanceRecords.add(new PerformanceRecord(startTime, "POST", latency, result.getStatusCode()));
                throughputRecords.add(new ThroughputRecord(endTime, 1));
                break;
            } catch (ApiException e) {
                handleApiException("POST", e);
            }
        }

    }

    private static void makeGetRequest() {
        long startTime = System.currentTimeMillis();
        DefaultApi apiInstance = new DefaultApi();
        apiInstance.getApiClient().setBasePath(IPAddr);

        // retry request for 5 times
        for (int i = 0; i < 5; i++) {
            try {
                ApiResponse<AlbumInfo> result = apiInstance.getAlbumByKey("123");
                // AlbumInfo result = apiInstance.getAlbumByKey(albumID);
                long endTime = System.currentTimeMillis();
                long latency = endTime - startTime;

                System.out.println("GET success");

                performanceRecords.add(new PerformanceRecord(startTime, "GET", latency, result.getStatusCode()));
                throughputRecords.add(new ThroughputRecord(endTime, 1));
                break;
            } catch (ApiException e) {
                handleApiException("GET", e);
                try {
                    Thread.sleep(1000); // 1 second pause
                } catch (InterruptedException ex) {
                    // Restore interrupted state...
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static void handleApiException(String method, ApiException e) {
        System.err.println("Exception when calling DefaultApi#" + method);
        e.printStackTrace();
    }

    private static void waitSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void printResults(long startTime, long endTime) {
        long wallTime = (endTime - startTime) / 1000;
        double throughput = (double) (threadGroupSize * numThreadGroups * 2000) / wallTime;

        System.out.println("Wall Time: " + wallTime + " seconds");
        System.out.println("Throughput: " + throughput + " requests per second");
    }


    private static class PerformanceRecord{
        private long startTime;
        private String requestType;
        private long latency;
        private int responseCode;

        public PerformanceRecord(long startTime, String requestType, long latency, int responseCode){
            this.startTime = startTime;
            this.requestType = requestType;
            this.latency = latency;
            this.responseCode = responseCode;
        }

        public long getStartTime(){
            return startTime;
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


    private static class ThroughputRecord {
        private long timestamp;
        private int requestCount;

        public ThroughputRecord(long timestamp, int requestCount) {
            this.timestamp = timestamp;
            this.requestCount = requestCount;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public int getRequestCount() {
            return requestCount;
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
}
