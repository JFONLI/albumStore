package dao;

import java.sql.*;
import java.util.List;
import java.util.UUID;

import org.apache.commons.dbcp2.*;
import dto.Album;

import javax.xml.transform.Result;

public class AlbumInfoDao {
    private static BasicDataSource dataSource;

    public AlbumInfoDao() {
        dataSource = DBCPDataSource.getDataSource();
    }

    public void createAlbum(List<Album> albums) throws SQLException {
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        String insertQueryStatement = "INSERT INTO Albums (imageBase64, info) " +
                "VALUES (?,?)";
        try {
            conn = dataSource.getConnection();
//            preparedStatement = conn.prepareStatement(insertQueryStatement);
//            preparedStatement.setString(1, newAlbum.getImageBase64());
//            preparedStatement.setString(2, newAlbum.getInfo());
//            // execute insert SQL statement
//            preparedStatement.executeUpdate();

            for (Album album : albums) {
                preparedStatement = conn.prepareStatement(insertQueryStatement);
                preparedStatement.setString(1, album.getImageBase64());
                preparedStatement.setString(2, album.getInfo());
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }catch (Exception e){
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
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
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        String queryStatement = "SELECT * FROM Albums WHERE albumId = ?";
        try {
            conn = dataSource.getConnection();
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

        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
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