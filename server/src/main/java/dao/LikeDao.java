package dao;

import java.sql.*;
import java.sql.PreparedStatement;

public class LikeDao {
    public int updateAlbumLikes(int albumId, int likesIncrement) {
        PreparedStatement preparedStatement = null;
        String updateQueryStatement = "UPDATE albums " +
                "SET likes = likes + ? " +
                "WHERE albumId = ?";
        int rowsAffected = 0;

        try (Connection conn = DataSource.getConnection()){
            preparedStatement = conn.prepareStatement(updateQueryStatement);
            preparedStatement.setInt(1, likesIncrement);
            preparedStatement.setInt(2, albumId);
            rowsAffected = preparedStatement.executeUpdate();
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
        return rowsAffected;
    }

    public int updateAlbumDislikes(int albumId, int dislikesIncrement) {
        PreparedStatement preparedStatement = null;
        String updateQueryStatement = "UPDATE albums " +
                "SET dislikes = dislikes + ? " +
                "WHERE albumId = ?";
        int rowsAffected = 0;

        try (Connection conn = DataSource.getConnection()){
            preparedStatement = conn.prepareStatement(updateQueryStatement);
            preparedStatement.setInt(1, dislikesIncrement);
            preparedStatement.setInt(2, albumId);
            rowsAffected = preparedStatement.executeUpdate();
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
        return rowsAffected;
    }
}
