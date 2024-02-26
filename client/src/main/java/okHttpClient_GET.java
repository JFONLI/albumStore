import okhttp3.*;
import java.io.*;

public class okHttpClient_GET {

    public static void main(String[] args) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Request request = new Request.Builder()
                .url("http://34.216.197.232:8080/albumStore/albums/8f368251-77f5-4701-a899-567a1a7a26e6")
                .method("GET", null)
                .build();
        Response response = client.newCall(request).execute();
        System.out.println(response.code());
        System.out.println(response.body());
    }
}
