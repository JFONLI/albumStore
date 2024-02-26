import okhttp3.*;

import java.io.File;
import java.io.IOException;

public class okHttpClient_POST {

    public static void main(String[] args) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart(
                        "image",
                        "image_example.jpg",
                        RequestBody.create(MediaType.parse("image/jpeg"),
                                new File("image_example.jpg"))
                )
                .addFormDataPart("profile", "{ \"artist\": \"Jimmy\", \"title\": \"NEU\", \"year\": \"0153\"}")
                .build();

        Request request = new Request.Builder()
                //.url("http://54.214.71.34:8080/albumStore/albums")
                .url("http://localhost:8080/albumStore_war_exploded/albums")
                .method("POST", body)
                .build();
        Response response = client.newCall(request).execute();

        System.out.println(response.code());
        System.out.println(response.body());
    }
}
