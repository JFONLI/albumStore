import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.DefaultApi;
import io.swagger.client.model.AlbumInfo;
import io.swagger.client.model.AlbumsProfile;
import io.swagger.client.model.ErrorMsg;
import io.swagger.client.model.ImageMetaData;

import java.io.File;

public class DefaultApiExampleGET {

    public static void main(String[] args) {

        DefaultApi apiInstance = new DefaultApi();
        // apiInstance.getApiClient().setBasePath("http://54.148.3.22:8080/albumStore");
        // apiInstance.getApiClient().setBasePath("http://54.185.63.202:8080/hw4");

        // apiInstance.getApiClient().setBasePath("http://localhost:8080");
        apiInstance.getApiClient().setBasePath("http://localhost:8080/albumStore_war_exploded");
        try {
            ApiResponse<AlbumInfo> result = apiInstance.getAlbumByKey("f86e4dac-0553-4323-8b44-6321a2341953");
            System.out.println(result.getData());

        } catch (ApiException e) {
            System.err.println("Exception when calling DefaultApi#getAlbumByKey");
            e.printStackTrace();
        }
    }
}