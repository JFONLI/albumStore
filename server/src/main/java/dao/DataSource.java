package dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DataSource {

    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;

//    private static final String HOST_NAME = System.getProperty("MySQL_IP_ADDRESS");
//    private static final String PORT = System.getProperty("MySQL_PORT");
//    private static final String DATABASE = "AlbumStore";
//    private static final String USERNAME = System.getProperty("DB_USERNAME");
//    private static final String PASSWORD = System.getProperty("DB_PASSWORD");

//    private static final String HOST_NAME = "albumstore.c5o0mau2uyhx.us-west-2.rds.amazonaws.com";
//    private static final String PORT = "3306";
//    private static final String USERNAME = "admin";
//    private static final String PASSWORD = "admin1998";
//    private static final String DATABASE = "AlbumStore";


    private static final String HOST_NAME = "localhost";
    private static final String PORT = "3306";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "02120407";
    private static final String DATABASE = "AlbumStore";

    static {
        try {
            String jdbcUrl = String.format("jdbc:mysql://%s:%s/%s?serverTimezone=America/Vancouver", HOST_NAME, PORT, DATABASE);
            config.setJdbcUrl( jdbcUrl );
            config.setUsername( USERNAME);
            config.setPassword( PASSWORD );
            config.addDataSourceProperty( "cachePrepStmts" , "true" );
            config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
            config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            ds = new HikariDataSource( config );
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    private DataSource() {}

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}