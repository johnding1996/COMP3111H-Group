package database.keeper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class which flatten a JSONObject to redis key-value, and handle the read/write operations.
 * Super class for {@link HistKeeper} and {@link LogKeeper}.
 * @author mcding
 * @version 1.2.1
 */
@Slf4j
public abstract class SerializeKeeper extends Keeper {
    /**
     * The list of fields for corresponding SerializeKeeper.
     */
    protected List<String> fields;

    /**
     * Set a key with JSONObject value.
     * @param key key
     * @param json JSONObject
     * @return whether add successfully or not
     */
    public abstract boolean set(String key, JSONObject json);

    /**
     * Get a JSONObject value of a key.
     * @param key key
     * @param number number of latest result to return
     * @return json JSONArray
     */
    public abstract JSONArray get(String key, int number);

    /**
     * Append a JSONObject to a list with specific key.
     * @param prefix key field prefix
     * @param key key
     * @param json JSONObject to append with
     * @return whether the appending operation is successful or not
     */
    protected boolean appendList(String prefix, String key, JSONObject json) {
        // Check validity
        Long statusCodeReply = jedis.rpush(prefix + ":" + key, json.toString());
        return (statusCodeReply > 0);
    }

    /**
     * Get the latest n records of a list with specific key.
     * @param prefix key field prefix
     * @param key key
     * @param number the number of latest records to get, positive integer
     * @return JSONArray as a list of latest several JSONObject records
     */
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

    /**
     * Get the latest several records before a timestamp of a list with specific key.
     * @param prefix key field prefix
     * @param key key
     * @param date timestamp
     * @return JSONArray as a list of latest several JSONObject records
     */
    protected JSONArray rangeList(String prefix, String key, Date date) {
        try {
            JSONArray completeArray = rangeList(prefix, key, 0);
            JSONArray array = new JSONArray();
            for (int i=0; i<completeArray.length(); i++) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                if (date.before(dateFormat.parse(completeArray.getJSONObject(i).getString("timestamp"))))
                    array.put(completeArray.getJSONObject(i));
            }
            return array;
        } catch (JSONException | ParseException e) {
            log.error("Failed to parse the timestamp in redis.", e);
            return null;
        }
    }

    /**
     * Utility method to shallow check a JSONObject by a list of fields.
     * @param jsonObject JSONObject to check
     * @param fields a list of fields
     * @return whether the JSONObject is valid or not
     */
    protected boolean checkValitidy(JSONObject jsonObject, List<String> fields) {
        for (String field: fields) {
            if (!jsonObject.has(field)){
                return false;
            }
        }
        return true;
    }

    /**
     * Utility method to shallow check a JSONArray by checking its JSONObjects one by one.
     * @param jsonArray JSONArray to check
     * @param fields a list of fields
     * @return whether the JSONArray is valid or not
     */
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
