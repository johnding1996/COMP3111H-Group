package controller;

import lombok.extern.slf4j.Slf4j;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.*;
import java.net.URISyntaxException;
import java.net.URI;

@Slf4j
public class SQLDatabaseEngine extends DatabaseEngine {
	@Override
	String search(String text) throws Exception {
    text = text.toLowerCase();
    log.info("SQL searching for {}", text);
    String ret = null;
    try {
      Connection connection = getConnection();
      PreparedStatement stmt = connection.prepareStatement(
          "SELECT keyword, response, hit_time FROM chatbotdb");
      ResultSet rs = stmt.executeQuery();
      while (rs.next()) {
        log.info("key = {}", rs.getString(1));
        if (text.contains(rs.getString(1).toLowerCase())) {
          ret = rs.getString(2);
          PreparedStatement updateStmt = connection.prepareStatement(
              "UPDATE chatbotdb SET hit_time=hit_time+1 WHERE keyword=?");
          updateStmt.setString(1, rs.getString(1));
          updateStmt.executeUpdate();
          ret += " (You have hit this item "+(rs.getInt(3)+1)+" time(s))";
          log.info("Result: {}", ret);
          break;
        }
      }
      rs.close();
      stmt.close();
      connection.close();
    } catch (Exception e) {
      log.info("Exception in searching for text {}: {}", text, e.toString());
    }
		if (ret != null) return ret;
    throw new Exception("NOT FOUND");
	}
	
	
	private Connection getConnection() throws URISyntaxException, SQLException {
		Connection connection;
		URI dbUri = new URI(System.getenv("DATABASE_URL"));

		String username = dbUri.getUserInfo().split(":")[0];
		String password = dbUri.getUserInfo().split(":")[1];
		String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath() +  "?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory";

		log.info("Username: {} Password: {}", username, password);
		log.info ("dbUrl: {}", dbUrl);
		
		connection = DriverManager.getConnection(dbUrl, username, password);

		return connection;
	}

}
