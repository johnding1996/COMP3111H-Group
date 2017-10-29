package database.keeper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link StateKeeper}
 * State keeper to read and write user's current state in the redis cache.
 * The valid states are defined by controller, which are
 * {"Idle", "ParseMenu", "AskMeal", "Recommend", "RecordMeal", "InitialInput", "Feedback"}
 * This state keeper will check the validity of state string.
 * @author mcding
 * @version 1.1
 */
@Slf4j
public class StateKeeper extends Keeper {
    /**
     * KEY_PREFIX
     * The prefix string of redis key.
     */
    private static final String KEY_PREFIX = "state";

    /**
     * STATE_STRINGS
     * The set of valid state strings.
     */
    private static final Set<String> STATE_STRINGS = new HashSet<>(
            Arrays.asList("Idle", "ParseMenu", "AskMeal", "Recommend", "RecordMeal", "InitialInput", "Feedback"));

    /**
     * lifeTime
     * This expire time of redis key.
     */
    private static int lifeTime = 3*60*60;

    StateKeeper() {
        super();
    }

    /**
     * checkValidity
     * Check validity of the state string.
     * @param state the state string
     * @return whether the state string is valid
     */
    private boolean checkValidity(String state) {
        return STATE_STRINGS.contains(state);
    }

    /**
     * get
     * Get the state string according to user id.
     * Return "Idle" if no user state stored.
     * @param uid user id
     * @return state string
     */
    public String get(int uid) {
        String state = jedis.get(KEY_PREFIX + ":" + Integer.toString(uid));
        // If no state value get, set it to Idle state
        if (state == null) {
            this.set(uid, "Idle");
            return "Idle";
        }
        // Check validity
        if (!checkValidity(state)) {
            log.error(String.format("Invalid state string %s when handling state loading for user %d", state, uid));
            // If invalid value is found, delete key and return null
            jedis.del(KEY_PREFIX + ":" + Integer.toString(uid));
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
        String statusCodeReply = jedis.setex(KEY_PREFIX + ":" + Integer.toString(uid), lifeTime, state);
        // If status code is "OK" then redis ensure that the value is stored
        return statusCodeReply.equals("OK");
    }

}
