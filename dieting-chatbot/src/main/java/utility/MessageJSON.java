package utility;

import org.json.JSONObject;

/**
 * Base class for FormatterMessageJSON and ParserMessageJSON.
 * @author szhouan
 * @version v2.0.0
 */
public abstract class MessageJSON {
    /**
     * JSONObject json initialization.
     */
    JSONObject json = new JSONObject();

    /**
     * Get pretty formatted String for MessageJSON.
     */
    @Override
    public String toString() {
        return json.toString(4);
    }

    /**
     * Get user Id of MessageJSON.
     * @return String of user Id.
     */
    public String getUserId() {
        return json.getString("userId");
    }

    /**
     * Get type of MessageJSON.
     * @return String of message type.
     */
    public String getType() {
        return json.getString("type");
    }
}