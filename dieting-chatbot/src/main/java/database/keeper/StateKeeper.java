package database.keeper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.lang.NullPointerException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

/**
 * State keeper to read and write user's current state in the redis cache.
 * The valid states are defined by controller, which are
 * {"Idle", "ParseMenu", "AskMeal", "Recommend", "RecordMeal", "InitialInput", "Feedback"}
 * This state keeper will check the validity of state string.
 * @author mcding
 * @version 1.1
 */
@Slf4j
public class StateKeeper {
    private static final String KEY_PREFIX = "state:";
    private static final Set<String> STATE_STRINGS = new HashSet<>(
            Arrays.asList("Idle", "ParseMenu", "AskMeal", "Recommend", "RecordMeal", "InitialInput", "Feedback"));
    private static int lifeTime;
    private static JedisPool pool;
    private Jedis jedis;
    static {
        //TODO: Config state lifeTime in a better way
        lifeTime = 3*60*60;
        try {
            URI uri = new URI(System.getenv("REDIS_URL"));
            pool = new JedisPool(new JedisPoolConfig(), uri);
            log.info("Redis pool initialized.");
        } catch (URISyntaxException e) {
            // If the URI is invalid
            pool = null;
            log.error("Invalid redis URI", e);
        }
    }

    /**
     * Constructor
     * connect to redis server and create the instance's jedis instance.
     * @throws JedisConnectionException when the redis pool cannot be connected
     * @throws NullPointerException when the redis pool isn't initialized
     */
    StateKeeper() throws JedisConnectionException, NullPointerException{
        jedis = pool.getResource();
    }

    /**
     * checkValidity
     * check validity of the state string
     * @param state the state string
     * @return whether the state string is valid
     */
    private boolean checkValidity(String state) {
        return STATE_STRINGS.contains(state);
    }

    /**
     * get
     * Get the state string according to user id.
     * Return "Idle" if no user state stored
     * @param uid user id
     * @return state string
     */
    public String get(int uid) {
        String state = jedis.get(KEY_PREFIX + Integer.toString(uid));
        // If no state value get, set it to Idle state
        if (state == null) {
            this.set(uid, "Idle");
            return "Idle";
        }
        // Check validity
        if (!checkValidity(state)) {
            log.error(String.format("Invalid state string %s when handling state loading for user %d", state, uid));
            // If invalid value is found, delete key and return null
            jedis.del(KEY_PREFIX + Integer.toString(uid));
            return null;
        } else {
            return state;
        }

    }

    /**
     * set
     * Set the user's state string according to user id.
     * Will check state string's validity.
     * @param uid user id
     * @param state state string
     * @return whether set successfully or not
     */
    public boolean set(int uid, String state) {
        // Check validity
        if (!checkValidity(state)) {
            log.error(String.format("Invalid state string %s when handling state storing for user %d", state, uid));
            return false;
        }
        String statusCodeReply = jedis.setex(KEY_PREFIX + Integer.toString(uid), lifeTime, state);
        // If status code is "OK" then redis ensure that the value is stored
        return statusCodeReply.equals("OK");
    }

}
