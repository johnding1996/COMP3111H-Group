package database.keeper;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

/**
 * Log keeper to store and load user interaction logs in the redis cache.
 * The valid JSONObject format is LogJSON defined by database APIs.
 * @author mcding
 * @version 1.2.1
 */
@Slf4j
public class LogKeeper extends SerializeKeeper {
    /**
     * The identifier of all user logs.
     */
    private static final String prefix = "log";
    private static final List<String> fields = Arrays.asList(
            "timestamp", "event", "old_state", "new_state"
    );

    /**
     * Default constructor.
     */
    public LogKeeper() {
        super();
    }

    /**
     * Constructor which uses external redis connection.
     * @param jedids external redis connection
     */
    LogKeeper(Jedis jedids) {
        this.jedis = jedis;
    }

    /**
     * Get the latest rows of user logs.
     * @param key key string
     * @param number number of latest result to return
     * @return JSONArray array of recent JSONObjects
     */
    @Override
    public JSONArray get(String key, int number) {
        JSONArray jsonArray = rangeList(prefix, key, number);
        if (jsonArray.length() == 0) {
            log.error("Attempting to search user log that does not exist.");
            return null;
        }
        if (!checkValidity(jsonArray, fields)) {
            log.error("Failed to load user meal history due to wrongly formatted LogJSON.");
            return null;
        }
        return jsonArray;
    }

    /**
     * Add new user log to cache.
     * @param key key string
     * @param logJson new row to add to the redis cache
     * @return whether appending operation is successful or not
     */
    public boolean set(String key, JSONObject logJson) {
        if (!checkValitidy(logJson, fields)) {
            log.error("Invalid formatted LogJSON.");
            return false;
        }
        return appendList(prefix, key, logJson);
    }
}
