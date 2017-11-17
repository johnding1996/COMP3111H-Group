package database.keeper;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

/**
 * Campaign Keeper to store coupon image, extension and count already claimed in the redis cache.
 * Campaign Keeper to store the sharing Code and Parent User Id
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
    private static final String KEY_EXT = "extension";
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
     * @return couponImg string
     */
    public String getCouponImg() {
        String couponImg = jedis.get(KEY_PREFIX + ":" + KEY_IMG);
        return couponImg;
    }

    /**
     * Set the coupon image according to the dumped image.
     * @param key key string
     * @return whether set successfully or not
     */
    public Boolean setCouponImg(String key) {
        String statusCodeReply = jedis.set(KEY_PREFIX + ":" + KEY_IMG, key);
        return statusCodeReply.equals("OK");
    }

    /**
     * Get the coupon image extension.
     * @return couponImg string
     */
    public String getCouponExt() {
        String couponExt = jedis.get(KEY_PREFIX + ":" + KEY_EXT);
        return couponExt;
    }

    /**
     * Set the coupon image extension according to the dumped image type.
     * @param key key string
     * @return whether set successfully or not
     */
    public Boolean setCouponExt(String key) {
        String statusCodeReply = jedis.set(KEY_PREFIX + ":" + KEY_EXT, key);
        return statusCodeReply.equals("OK");
    }

    /**
     * Get the coupon count.
     * @return couponCnt int
     */
    public String getCouponCnt() {
        String couponCnt = jedis.get(KEY_PREFIX + ":" + KEY_CNT);
        return couponCnt;
    }

    /**
     * Reset the coupon count to 1.
     * @return whether reset successfully or not
     */
    public Boolean resetCouponCnt() {
        String statusCodeReply = jedis.set(KEY_PREFIX + ":" + KEY_CNT, "0");
        return statusCodeReply.equals("OK");
    }

    /**
     * Increment the coupon count by 1.
     * @return the new coupon count Long
     */
    public Long incrCouponCnt() {

        return jedis.incr(KEY_PREFIX + ":" + KEY_CNT);
    }

    /**
     * Set the parentUserId according to new user id and parent user id.
     * @param key sharing code
     * @param parentUserId parent User Id
     * @return whether set successfully or not
     */
    public Boolean setParentUserId(String key, String parentUserId) {
        if (!(checkValidityCode(key) && checkValidityUserId(parentUserId))) return false;
        String statusCodeReply = jedis.set(KEY_PREFIX + ":" + KEY_PARENT + ":" + key, parentUserId);
        return statusCodeReply.equals("OK");
    }

    /**
     * Get the parentUserId according to new user id and parent user id.
     * @param key sharing code
     * @return the parent user id
     */
    public String getParentUserId(String key) {
        if (!checkValidityCode(key)) return null;
        return jedis.get(KEY_PREFIX + ":" + KEY_PARENT + ":" + key);
    }


}