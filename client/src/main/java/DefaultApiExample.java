import io.swagger.client.*;
import io.swagger.client.auth.*;
import io.swagger.client.model.*;
import io.swagger.client.api.DefaultApi;

import java.io.File;
import java.util.*;

public class DefaultApiExample {

    public static void main(String[] args) {

        DefaultApi apiInstance = new DefaultApi();
        apiInstance.getApiClient().setBasePath("http://54.148.3.22:8080/albumStore");
        // apiInstance.getApiClient().setBasePath("http://localhost:8080");
        // apiInstance.getApiClient().setBasePath("http://localhost:8080/albumStore_war_exploded");
        File image = new File("image_example.jpg"); // File |
        AlbumsProfile profile = new AlbumsProfile(); // AlbumsProfile |
        profile.setArtist("Jerry");
        profile.setTitle("Hello");
        profile.setYear("1998");
        try {
            ApiResponse<ImageMetaData> result = apiInstance.newAlbum(image, profile);
            // ImageMetaData result = apiInstance.newAlbum(image, profile);
            System.out.println(result.getData());

        } catch (ApiException e) {
            System.err.println("Exception when calling DefaultApi#newAlbum");
            e.printStackTrace();
        }
    }
}