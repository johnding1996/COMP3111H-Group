package controller;

import java.util.HashSet;
import org.json.JSONObject;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import controller.StateMachine;

import lombok.extern.slf4j.Slf4j;

/**
 * Wrapper class for LINE message
 */
@Slf4j
@Service
public class ParserMessageJSON extends MessageJSON {
    static private HashSet<String> keySet;
    {
        keySet = new HashSet<String>();
        keySet.add("userId");
        keySet.add("state");
        keySet.add("replyToken");
        keySet.add("message");
    }

    private JSONObject jo = new JSONObject();

    private static boolean isKey(String key) {
        return keySet.contains(key);
    }

    private static boolean validateKeyValue(String key, Object value) {
        switch (key) {
            case "userId": case "state": case "replyToken":
                if (!(value instanceof String)) return false;
                String str = (String)value;
                if (key.equals("state")) {
                    if (!State.validateStateName(str)) return false;
                }
                break;

            default:
                return false;
        }
        return true;
    }

    /**
     * get a field of ParserMessageJSON
     * @param key String of key
     * @return The corresponding value of key in Object
     *         Return null if no such key
     */
    public String get(String key) {
        if (!isKey(key) || key.equals("message")) {
            log.info(String.format("%s is not a valid key", key));
            return null;
        } else {
            return (String)jo.get(key);
        }
    }

    /**
     * set a field of ParserMessageJSON
     * @param key String of key, only some values are allowed
     * @param value Object to be set
     * @return This object
     */
    public ParserMessageJSON set(String key, Object value) {
        if (!isKey(key)) {
            log.info(String.format("%s is not a valid key", key));
        } else {
            if (validateKeyValue(key, value))
                jo.put(key, value);
        }
        return this;
    }

    /**
     * set content of text message
     * @param id LINE message id in String
     * @param textContent String of text message
     * @return This object
     */
    public ParserMessageJSON setTextMessage(String id, String text) {
        JSONObject msg = new JSONObject();
        msg.put("id", id);
        msg.put("type", "text");
        msg.put("textContent", text);
        jo.put("message", msg);
        return this;
    }

    /**
     * set content of image message
     * @param id LINE message id in String
     * @return This object
     */
    public ParserMessageJSON setImageMessage(String id) {
        JSONObject msg = new JSONObject();
        msg.put("id", id);
        msg.put("type", "image");
        jo.put("message", msg);
        return this;
    }

    /**
     * Get message type
     * @return Type of LINE message, either "text" or "image"
     */
    public String getMessageType() {
        try {
            JSONObject msg = (JSONObject)jo.get("message");
            String ret = (String)msg.get("type");
            return ret;
        } catch (Exception e) {
            log.info("Error occurs in getting message type");
            return null;
        }
    }

    /**
     * Get text message content
     * @return String of message text, null if is not a text message
     */
    public String getTextContent() {
        if (!getMessageType().equals("text")) return null;
        try {
            JSONObject msg = (JSONObject)jo.get("message");
            String ret = (String)msg.get("textContent");
            return ret;
        } catch (Exception e) {
            log.info("Error in getting text content");
            return null;
        }
    }

    /**
     * Get image message content
     * @return String (to be changed), null if not an image
     */
    public String getImageContent() {
        if (!getMessageType().equals("image")) return null;
        try {
            JSONObject msg = (JSONObject)jo.get("message");
            String ret = (String)msg.get("id");
            return ret;
        } catch (Exception e) {
            log.info("Error in getting image content");
            return null;
        }
    }

    /**
     * Return a pretty formatted JSON
     */
    @Override
    public String toString() {
        return "\n"+jo.toString(4);
    }
}