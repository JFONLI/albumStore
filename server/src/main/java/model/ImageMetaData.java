package model;

public class ImageMetaData {
    private String albumID;
    private String imageSize;

    public String getImageSize() {
        return imageSize;
    }

    public String getAlbumID() {
        return albumID;
    }

    public void setImageSize(String imageSize) {
        this.imageSize = imageSize;
    }

    public void setAlbumID(String albumID) {
        this.albumID = albumID;
    }
}
