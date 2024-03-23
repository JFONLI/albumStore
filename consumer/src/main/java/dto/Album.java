package dto;

import java.util.UUID;

public class Album {
    // private UUID albumId;
    private int albumId;
    private String imageBase64;
    private String info;

    public Album(int albumId, String imageBase64, String info) {
        this.albumId = albumId;
        this.imageBase64 = imageBase64;
        this.info = info;
    }

    public int getAlbumId() {
        return albumId;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setAlbumId(int albumId) {
        this.albumId = albumId;
    }

    public String getInfo() {
        return info;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
