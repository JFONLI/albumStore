//import io.swagger.client.*;
//import io.swagger.client.auth.*;
//import io.swagger.client.model.*;
//import io.swagger.client.api.DefaultApi;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.CountDownLatch;
//
//public class LoadTestProgram {
//    private static int threadGroupSize;
//    private static int numThreadGroups;
//    private static int delay;
//    private static String IPAddr;
//
//    public static void main(String[] args) {
////        if (args.length != 4) {
////            System.out.println("Usage: java LoadTestProgram <threadGroupSize> <numThreadGroups> <delay> <IPAddr>");
////            System.exit(1);
////        }
//
////        threadGroupSize = Integer.parseInt(args[0]);
////        numThreadGroups = Integer.parseInt(args[1]);
////        delay = Integer.parseInt(args[2]);
////        IPAddr = args[3];
////
////
//        // Step 1
//        threadGroupSize = 10;
//        numThreadGroups = 30;
//        delay = 2;
//        // IPAddr = "http://localhost:8080/hw4_war_exploded/";
//        // AWS - Tomcat
//        IPAddr =  "http://54.202.210.12:8080/hw4";
//        // IPAddr =  "http://34.219.156.125:8080";
//
//        runLoadTest();
//    }
//
//    private static void runLoadTest() {
//        System.out.println("Start testing....");
//        // Step 2
//        initializeThreads(10, 100);
//
//        System.out.println("hardcoded initialization completed");
//
//        System.out.println("Start main testing");
//        // Step 3
//        long startTime = System.currentTimeMillis();
//
//        // Step 4
//        for (int i = 0; i < numThreadGroups; i++) {
//            initializeThreads(threadGroupSize, 1000);
//            waitSeconds(delay);
//            System.out.println("Start " + i + " Group");
//        }
//
//        long endTime = System.currentTimeMillis();
//
//        printResults(startTime, endTime);
//    }
//
//    private static void initializeThreads(int numThreads, int numIterations) {
//        CountDownLatch latch = new CountDownLatch(numThreads);
//        List<Thread> threads = new ArrayList<>();
//
//        for (int i = 0; i < numThreads; i++) {
//            Thread thread = new Thread(() -> {
//                for (int j = 0; j < numIterations; j++) {
//                    makePostRequest();
//                    makeGetRequest();
//                }
//                latch.countDown();
//            });
//
//            threads.add(thread);
//            thread.start();
//        }
//
//        try {
//            latch.await();
//        } catch (InterruptedException e){
//            e.printStackTrace();
//        }
//    }
//
//    private static void makePostRequest() {
//        DefaultApi apiInstance = new DefaultApi();
//        apiInstance.getApiClient().setBasePath(IPAddr);
//
//        File image = new File("image_example.jpg");
//        AlbumsProfile profile = new AlbumsProfile();
//
//        // retry request for 5 times
//        for (int i = 0; i < 5; i++) {
//            try {
//                ImageMetaData result = apiInstance.newAlbum(image, profile);
//                break;
//            } catch (ApiException e) {
//                handleApiException("POST", e);
//            }
//        }
//    }
//
//    private static void makeGetRequest() {
//        DefaultApi apiInstance = new DefaultApi();
//        apiInstance.getApiClient().setBasePath(IPAddr);
//
//        String albumID = "albumID_example";
//
//        // retry request for 5 times
//        for (int i = 0; i < 5; i++) {
//            try {
//                AlbumInfo result = apiInstance.getAlbumByKey(albumID);
//                break;
//            } catch (ApiException e) {
//                handleApiException("GET", e);
//            }
//        }
//    }
//
//    private static void handleApiException(String method, ApiException e) {
//        System.err.println("Exception when calling DefaultApi#" + method);
//        e.printStackTrace();
//    }
//
//    private static void waitSeconds(int seconds) {
//        try {
//            Thread.sleep(seconds * 1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private static void printResults(long startTime, long endTime) {
//        long wallTime = (endTime - startTime) / 1000;
//        double throughput = (double) (threadGroupSize * numThreadGroups * 2000) / wallTime;
//
//        System.out.println("Wall Time: " + wallTime + " seconds");
//        System.out.println("Throughput: " + throughput + " requests per second");
//    }
//}
