package dao;

import java.sql.*;
import java.util.List;

import dto.Album;

public class AlbumInfoDao {

    public void createAlbumBatch(List<Album> albums) throws SQLException {
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

    public Album findByAlbumId(int albumId){
        Album album = null;
        // Connection conn = null;
        PreparedStatement preparedStatement = null;
        String queryStatement = "SELECT * FROM Albums WHERE albumId = ?";
        try (Connection conn = DataSource.getConnection()){
            // conn = DataSource.getConnection();
            preparedStatement = conn.prepareStatement(queryStatement);
            preparedStatement.setInt(1, albumId);

            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                String imageBase64 = resultSet.getString("imageBase64");
                String info = resultSet.getString("info");

                album = new Album(1, imageBase64, info);
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