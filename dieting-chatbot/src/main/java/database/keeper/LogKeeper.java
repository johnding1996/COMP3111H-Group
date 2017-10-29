package database.keeper;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

/**
 * {@link LogKeeper}
 * Log keeper to store and load user interaction logs in the redis cache.
 * The valid JSONObject format is LogJSON defined by database APIs.
 * @author mcding
 * @version 1.1
 */
@Slf4j
public class LogKeeper extends SerializeKeeper {
    /**
     * prefix
     * The identifier of all user logs.
     */
    private static final String prefix = "log";
    private static final List<String> fields = Arrays.asList(
            "timestamp", "event", "old_state", "new_state"
    );

    /**
     * constructor
     * Default constructor.
     */
    LogKeeper() {
        super();
    }

    /**
     * constructor
     * Constructor which uses external redis connection.
     * @param jedids external redis connection
     */
    LogKeeper(Jedis jedids) {
        this.jedis = jedis;
    }

    /**
     * get
     * Get the latest rows of user logs.
     * @param user_id user id
     * @param number number of latest result to return
     * @return JSONArray array of recent JSONObjects
     */
    @Override
    public JSONArray get(String user_id, int number) {
        JSONArray jsonArray = rangeList(prefix, user_id, number);
        if (jsonArray.length() == 0) {
            log.error("Attempting to get user log that does not exist.");
            return null;
        }
        if (!checkValidity(jsonArray, fields)) {
            log.error("Failed to load user meal history due to wrongly formatted LogJSON.");
            return null;
        }
        return jsonArray;
    }

    /**
     * add
     * Add new user log to cache.
     * @param user_id user id
     * @param logJson new row to add to the redis cache
     * @return whether appending operation is successful or not
     */
    public boolean set(String user_id, JSONObject logJson) {
        if (!checkValitidy(logJson, fields)) {
            log.error("Invalid formatted LogJSON.");
            return false;
        }
        return appendList(prefix, user_id, logJson);
    }
}
