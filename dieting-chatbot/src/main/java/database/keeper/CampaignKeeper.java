package database.keeper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

/**
 * Campaign Keeper to store coupon image and number of coupon already claimed in the redis cache.
 * Campaign Keeper to store the sharing Code and Parent User Id
 * The valid JSONObject format is CampaignJSON defined by database APIs.
 * @author wguoaa
 * @version 1.3.1
 */
@Slf4j
public class CampaignKeeper extends Keeper {
    /**
     * The prefix string of redis key.
     */
    private static final String KEY_PREFIX = "campaign";
    private static final String KEY_IMG = "image";
    private static final String KEY_CNT = "count";
    private static final String KEY_CODE = "code";
    private static final String KEY_PARENT = "parent";

    /**
     * Default constructor.
     */
    public CampaignKeeper() {
        super();
    }

    CampaignKeeper(Jedis jedids) {
        this.jedis = jedis;
    }

    /**
     * Get the coupon image.
     *
     * @return couponImg string
     */
    public String getCouponImg() {
        String couponImg = jedis.get(KEY_PREFIX + ":" + KEY_IMG);
        return couponImg;
    }

    /**
     * Set the coupon image according to the dumped image.
     *
     * @param key key string
     * @return whether set successfully or not
     */
    public Boolean setCouponImg(String key) {
        String statusCodeReply = jedis.set(KEY_PREFIX + ":" + KEY_IMG, key);
        return statusCodeReply.equals("OK");
    }

    /**
     * Get the coupon count.
     *
     * @return couponCnt int
     */
    public String getCouponCnt() {
        String couponCnt = jedis.get(KEY_PREFIX + ":" + KEY_CNT);
        return couponCnt;
    }

    /**
     * Reset the coupon count to 1.
     *
     * @return whether reset successfully or not
     */
    public Boolean resetCouponCnt() {
        String statusCodeReply = jedis.set(KEY_PREFIX + ":" + KEY_CNT, "1");
        return statusCodeReply.equals("OK");
    }

    /**
     * Increment the coupon count by 1.
     *
     * @return the new coupon count Long
     */
    public Long incrCouponCnt() {

        return jedis.incr(KEY_PREFIX + ":" + KEY_CNT);
    }

    /**
     * Set the sharingCode according to userid and the code.
     *
     * @param key  userid
     * @param code sharing code
     * @return whether set successfully or not
     */
    public Boolean setSharingCode(String key, String code) {
        String statusCodeReply = jedis.set(KEY_PREFIX + ":" + KEY_CODE + ":" + key, code);
        return statusCodeReply.equals("OK");
    }

    /**
     * Get the sharingCode according to userid and the code.
     *
     * @param key userid
     * @return the sharing code
     */
    public String getSharingCode(String key) {

        return jedis.get(KEY_PREFIX + ":" + KEY_CODE + ":" + key);
    }

    /**
     * Set the parentUserId according to new user id and parent user id.
     *
     * @param key          userid
     * @param parentUserId parent User Id
     * @return whether set successfully or not
     */
    public Boolean setParentUserId(String key, String parentUserId) {
        String statusCodeReply = jedis.set(KEY_PREFIX + ":" + KEY_PARENT + ":" + key, parentUserId);
        return statusCodeReply.equals("OK");
    }

    /**
     * Get the parentUserId according to new user id and parent user id.
     *
     * @param key userid
     * @return the parent user id
     */
    public String getParentUserId(String key) {

        return jedis.get(KEY_PREFIX + ":" + KEY_PARENT + ":" + key);
    }

//    /**
//     * Get the latest rows of campaign actions.
//     * @param key key string
//     * @param number number of latest result to return
//     * @return JSONArray array of recent JSONObjects
//     */
//    @Override
//    public JSONArray get(String key, int number) {
//        JSONArray jsonArray = rangeList(prefix, key, number);
//        if (jsonArray.length() == 0) {
//            log.error("Attempting to search campaign action that does not exist.");
//            return null;
//        }
//        if (!checkValidity(jsonArray, fields)) {
//            log.error("Failed to load campaign action due to wrongly formatted CampaignJSON.");
//            return null;
//        }
//        return jsonArray;
//    }
//
//    /**
//     * Add new campaign action to cache.
//     * @param key key string
//     * @param queryJson new row to add to the redis cache
//     * @return whether appending operation is successful or not
//     */
//    public boolean set(String key, JSONObject queryJson) {
//        if (!checkValitidy(queryJson, fields)) {
//            log.error("Invalid formatted CampaignJSON.");
//            return false;
//        }
//        return appendList(prefix, key, queryJson);
//    }
}