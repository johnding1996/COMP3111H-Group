package database.keeper;

import java.util.List;

import org.json.JSONObject;
import org.json.JSONArray;

import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class which flatten a JSONObject to redis key-value, and handle the read/write operations.
 * Super class for {@link HistKeeper} and {@link LogKeeper}.
 * @author mcding
 * @version 1.2
 */
@Slf4j
public abstract class SerializeKeeper extends Keeper {
    protected List<String> fields;

    /**
     * @param key key
     * @param json JSONObject
     * @return whether add successfully or not
     */
    public abstract boolean set(String key, JSONObject json);

    /**
     * @param key key
     * @param number number of latest result to return
     * @return json JSONArray
     */
    public abstract JSONArray get(String key, int number);


    protected boolean appendList(String prefix, String key, JSONObject json) {
        // Check validity
        Long statusCodeReply = jedis.rpush(prefix + ":" + key, json.toString());
        return (statusCodeReply > 0);
    }

    protected JSONArray rangeList(String prefix, String key, int number) {
        List<String> values = jedis.lrange(prefix + ":" + key, -number, -1);
        if (values == null) {
            return null;
        }
        JSONArray jsonArray = new JSONArray();
        for (String value: values) {
            JSONObject jsonObject= new JSONObject(value);
            jsonArray.put(jsonObject);
        }
        return jsonArray;
    }

    protected boolean checkValitidy(JSONObject jsonObject, List<String> fields) {
        for (String field: fields) {
            if (!jsonObject.has(field)){
                return false;
            }
        }
        return true;
    }

    protected boolean checkValidity(JSONArray jsonArray, List<String> fields) {
        for (int i=0; i<jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            if (!checkValitidy(jsonObject, fields)){
                return false;
            }
        }
        return true;
    }
}
