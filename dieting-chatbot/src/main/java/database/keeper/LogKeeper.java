package database.keeper;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

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
     * get
     * Get the latest rows of user logs.
     * @param key key
     * @param number number of latest result to return
     * @return JSONArray array of recent JSONObjects
     */
    @Override
    public JSONArray get(String key, int number) {
        JSONArray jsonArray = rangeList(prefix, key, number);
        if (!checkValidity(jsonArray, fields)) {
            log.error("Failed to load user meal history due to wrongly formatted LogJSON.");
            return null;
        }
        return jsonArray;
    }

    /**
     * set
     * Add new user log to cache.
     * @param key key
     * @param jsonObject new row to add to the redis cache
     * @return whether appending operation is successful or not
     */
    public boolean set(String key, JSONObject jsonObject) {
        if (!checkValitidy(jsonObject, fields)) {
            log.error("Invalid formatted LogJSON.");
            return false;
        }
        return appendList(prefix, key, jsonObject);
    }
}
