package database.keeper;

import controller.State;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

/**
 * State keeper to read and write user's current state in the redis cache.
 * The valid states are defined by controller, which are
 * {"Idle", "ParseMenu", "AskMeal", "Recommend", "RecordMeal", "InitialInput", "Feedback"}
 * This state keeper will check the validity of state string.
 * @author mcding
 * @version 1.2.1
 */
@Slf4j
public class StateKeeper extends Keeper {
    /**
     * The prefix string of redis key.
     */
    private static final String KEY_PREFIX = "state";

    /**
     * This expire time of redis key.
     */
    private int lifeTime = 3*60*60;

    /**
     * Default constructor.
     */
    public StateKeeper() {
        super();
    }

    StateKeeper(Jedis jedids, int lifeTime) {
        this.jedis = jedis;
        this.lifeTime = lifeTime;
    }

    /**
     * Check validity of the state string.
     * @param state the state string
     * @return whether the state string is valid
     */
    private boolean checkValidity(String state) {
        return State.validateStateName(state);
    }

    /**
     * Get the state string according to user id.
     * Return "Idle" if no user state stored.
     * @param key key string
     * @return state string
     */
    public String get(String key) {
        String state = jedis.get(KEY_PREFIX + ":" + key);
        // If no state value search, add it to Idle state
        if (state == null) {
            this.set(key, "Idle");
            return "Idle";
        }
        // Check validity
        if (!checkValidity(state)) {
            log.error(String.format("Invalid state string %s when handling state loading for user %s", state, key));
            // If invalid value is found, delete key and return null
            jedis.del(KEY_PREFIX + ":" + key);
            return null;
        } else {
            return state;
        }
    }

    /**
     * Set the user's state string according to user id.
     * Will check state string's validity.
     * @param key key string
     * @param state state string
     * @return whether add successfully or not
     */
    public boolean set(String key, String state) {
        // Check validity
        if (!checkValidity(state)) {
            log.error(String.format("Invalid state string %s when handling state storing for user %s", state, key));
            return false;
        }
        String statusCodeReply = jedis.setex(KEY_PREFIX + ":" + key, lifeTime, state);
        // If status code is "OK" then redis ensure that the value is stored
        return statusCodeReply.equals("OK");
    }
}
