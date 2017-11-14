package utility;

import org.json.JSONObject;

/**
 * Base class for FormatterMessageJSON and ParserMessageJSON.
 * @author szhouan
 * @version v2.0.0
 */
public abstract class MessageJSON {
    JSONObject json = new JSONObject();

    /**
<<<<<<< HEAD
     * Get pretty formatted String for MessageJSON
=======
     * Get pretty formatted String for MessageJSON.
>>>>>>> develop
     */
    @Override
    public String toString() {
        return json.toString(4);
    }

    /**
<<<<<<< HEAD
     * Get user Id of MessageJSON
     * @return String of user Id
=======
     * Get user Id of MessageJSON.
     * @return String of user Id.
>>>>>>> develop
     */
    public String getUserId() {
        return json.getString("userId");
    }

    /**
<<<<<<< HEAD
     * Get type of MessageJSON
     * @return String of message type
=======
     * Get type of MessageJSON.
     * @return String of message type.
>>>>>>> develop
     */
    public String getType() {
        return json.getString("type");
    }
}