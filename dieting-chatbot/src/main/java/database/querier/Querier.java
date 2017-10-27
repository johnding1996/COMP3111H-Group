package database.querier;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.*;
import redis.clients.jedis.Jedis;
import java.net.URISyntaxException;
import java.net.URI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author      mcding
 */
public abstract class Querier {
    private final URI REDIS_URL;
    private final URI DATABASE_URL;


    Querier() throws URISyntaxException {
        REDIS_URL = new URI(System.getenv("REDIS_URL"));
        DATABASE_URL = new URI(System.getenv("DATABASE_URL"));

    }

    private Jedis getRedisConnection() throws URISyntaxException {
        return new Jedis(REDIS_URL);
    }

    private Connection getSQLConnection() throws URISyntaxException, SQLException {
        String username = DATABASE_URL.getUserInfo().split(":")[0];
        String password = DATABASE_URL.getUserInfo().split(":")[1];
        String dbUrl = "jdbc:postgresql://" + DATABASE_URL.getHost() + ':' + DATABASE_URL.getPort() + DATABASE_URL.getPath() +  "?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory";
        return DriverManager.getConnection(dbUrl, username, password);
    }

    public JSONArray get(String key) {
        return new JSONArray();
    }

    public void set(String key, JSONArray a) {

    }

    private String exactSearch(String text) throws Exception {
        text = text.toLowerCase();
        String ret = null;
        try {
            Connection connection = this.getSQLConnection();
            PreparedStatement stmt = connection.prepareStatement("SELECT keyword, response, hit_time FROM chatbotdb");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                if (text.contains(rs.getString(1).toLowerCase())) {
                    ret = rs.getString(2);
                    PreparedStatement updateStmt = connection.prepareStatement(
                            "UPDATE chatbotdb SET hit_time=hit_time+1 WHERE keyword=?");
                    updateStmt.setString(1, rs.getString(1));
                    updateStmt.executeUpdate();
                    ret += " (You have hit this item "+(rs.getInt(3)+1)+" time(s))";
                    break;
                }
            }
            rs.close();
            stmt.close();
            connection.close();
        } catch (Exception e) {

        }
        if (ret != null) return ret;
        throw new Exception("NOT FOUND");
    }
}
