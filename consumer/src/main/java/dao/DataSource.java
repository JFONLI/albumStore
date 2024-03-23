package dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DataSource {

    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;


//    private static final String PORT = System.getProperty("MySQL_PORT");
//    private static final String USERNAME = System.getProperty("DB_USERNAME");
//    private static final String PASSWORD = System.getProperty("DB_PASSWORD");
//    private static final String HOST_NAME = System.getProperty("MySQL_IP_ADDRESS");
    private static final String PORT = "3306";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "02120407";
    private static final String HOST_NAME = "database-1.czqu4aki8hux.us-west-2.rds.amazonaws.com";
    // private static final String HOST_NAME = "localhost";
    private static final String DATABASE = "albumstore";

    static {
        try {
            String jdbcUrl = String.format("jdbc:mysql://%s:%s/%s?serverTimezone=America/Vancouver", HOST_NAME, PORT, DATABASE);
            //String jdbcUrl = String.format("jdbc:mysql://%s:%s/%s", HOST_NAME, PORT, DATABASE);
            System.out.println(jdbcUrl);
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