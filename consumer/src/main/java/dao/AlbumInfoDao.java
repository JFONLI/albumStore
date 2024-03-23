package dao;

import java.sql.*;
import java.util.List;

import dto.Album;

public class AlbumInfoDao {

    public void createAlbumBatch(List<Album> albums) {
        // Connection conn = null;
        PreparedStatement preparedStatement = null;
        String insertQueryStatement = "INSERT INTO Albums (imageBase64, info) " +
                "VALUES (?,?)";

        try (Connection conn = DataSource.getConnection()){
            preparedStatement = conn.prepareStatement(insertQueryStatement);
            for(Album album : albums){
                preparedStatement.setString(1, album.getImageBase64());
                preparedStatement.setString(2, album.getInfo());
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            try {
//                if (conn != null) {
//                    conn.close();
//                }
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }

    public int createAlbum(Album album){
        // Connection conn = null;
        PreparedStatement preparedStatement = null;
        String insertQueryStatement = "INSERT INTO albums (imageBase64, info) " +
                "VALUES (?,?)";

        try (Connection conn = DataSource.getConnection()){
            preparedStatement = conn.prepareStatement(insertQueryStatement,  Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, album.getImageBase64());
            preparedStatement.setString(2, album.getInfo());
            preparedStatement.executeUpdate();

            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            // System.out.println(generatedKeys);
            if (generatedKeys.next()) {
                int generatedId = generatedKeys.getInt(1); // 或者使用列名：generatedKeys.getInt("id");
                // System.out.println("Primary Key is：" + generatedId);
                return generatedId;
            } else {
                System.out.println("Failed to get primary key");
            }
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            try {
//                if (conn != null) {
//                    conn.close();
//                }
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
        return -1;
    }

    public Album findByAlbumId(int albumId){
        Album album = null;
        // Connection conn = null;
        PreparedStatement preparedStatement = null;
        String queryStatement = "SELECT * FROM albums WHERE albumId = ?";
        try (Connection conn = DataSource.getConnection()){
            // conn = DataSource.getConnection();
            preparedStatement = conn.prepareStatement(queryStatement);
            preparedStatement.setInt(1, albumId);

            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                String imageBase64 = resultSet.getString("imageBase64");
                String info = resultSet.getString("info");

                album = new Album(albumId, imageBase64, info);
            } else {
                return null;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
//                if (conn != null) {
//                    conn.close();
//                }
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
        return album;
    }
}