package database.connection;

import java.sql.Connection;
import java.sql.SQLException;
import org.apache.commons.dbcp2.BasicDataSource;

import java.net.URI;
import java.net.URISyntaxException;

import lombok.extern.slf4j.Slf4j;

/**
 * Statically keep the SQL connection pool.
 * Provide wrapper method to generate new connection and close connection.
 * @author mcding
 * @version 1.2
 */
@Slf4j
public class SQLPool {
    private static BasicDataSource connectionPool;

    static {
        try {
            URI dbUri = new URI(System.getenv("DATABASE_URL"));
            String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort()
                    + dbUri.getPath() + "?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory";
            connectionPool = new BasicDataSource();
            if (dbUri.getUserInfo() != null) {
                connectionPool.setUsername(dbUri.getUserInfo().split(":")[0]);
                connectionPool.setPassword(dbUri.getUserInfo().split(":")[1]);
            }
            connectionPool.setDriverClassName("org.postgresql.Driver");
            connectionPool.setUrl(dbUrl);
            connectionPool.setInitialSize(1);
        } catch (URISyntaxException e) {
            connectionPool = null;
            log.error("Invalid PostgreSQL URI", e);
        }
    }

    /**
     * Return a new SQL JDBC connection
     * @return connection SQL JDBC connection
     */
    public static Connection getConnection() {
        try {
            return connectionPool.getConnection();
        } catch (NullPointerException e) {
            log.error("SQL pool has not been properly initialized.", e);
            return null;
        } catch (SQLException e) {
            log.error("Failed to establish SQL connection.", e);
            return null;
        }

    }

    /**
     * Close a SQL connection.
     * @param connection SQL connection
     */
    public static void closeConnection(Connection connection) {
        try{
            connection.close();
        } catch (SQLException e) {
            log.error("Failed to close SQL connection.", e);

        }
    }


}
