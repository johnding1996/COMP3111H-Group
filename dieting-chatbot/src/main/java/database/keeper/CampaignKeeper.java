package database.keeper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

/**
 * Campaign Keeper to store coupon image and number of coupon already claimed in the redis cache.
 * The valid JSONObject format is CampaignJSON defined by database APIs.
 * @author wguoaa
 * @version 1.3.1
 */
@Slf4j
public class CampaignKeeper extends SerializeKeeper {
    /**
     * The identifier of all campaign actions.
     */
    private static final String prefix = "campaign";

    /**
     * The valid fields of CampaignJSON.
     */
    private static final List<String> fields = Arrays.asList(
            "couponImg", "couponCnt"
    );

    /**
     * constructor.
     * Default constructor.
     */
    public CampaignKeeper() {
        super();
    }

    /**
     * Constructor which uses external redis connection.
     * @param jedids external redis connection
     */
    CampaignKeeper(Jedis jedids) {
        this.jedis = jedis;
    }

    /**
     * Get the latest rows of campaign actions.
     * @param key key string
     * @param number number of latest result to return
     * @return JSONArray array of recent JSONObjects
     */
    @Override
    public JSONArray get(String key, int number) {
        JSONArray jsonArray = rangeList(prefix, key, number);
        if (jsonArray.length() == 0) {
            log.error("Attempting to search campaign action that does not exist.");
            return null;
        }
        if (!checkValidity(jsonArray, fields)) {
            log.error("Failed to load campaign action due to wrongly formatted CampaignJSON.");
            return null;
        }
        return jsonArray;
    }

    /**
     * Add new campaign action to cache.
     * @param key key string
     * @param queryJson new row to add to the redis cache
     * @return whether appending operation is successful or not
     */
    public boolean set(String key, JSONObject queryJson) {
        if (!checkValitidy(queryJson, fields)) {
            log.error("Invalid formatted CampaignJSON.");
            return false;
        }
        return appendList(prefix, key, queryJson);
    }
}
