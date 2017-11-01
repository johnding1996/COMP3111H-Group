package controller;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.context.annotation.Bean;

import reactor.Environment;
import reactor.bus.Event;
import reactor.bus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FormatterMessageJSON extends MessageJSON {
    static private HashSet<String> keySet;
    {
        keySet = new HashSet<String>();
        keySet.add("type");
        keySet.add("userId");
        keySet.add("replyToken");
        keySet.add("messages");
        keySet.add("stateTransition");
    }

    private JSONObject jo = new JSONObject();
    {
        jo.put("messages", new JSONArray());
    }

    private static boolean isKey(String key) {
        return keySet.contains(key);
    }
    
    private static boolean validateKeyValue(String key, Object value) {
        switch (key) {
            case "type": case "userId":
            case "replyToken": case "stateTransition":
                if (!(value instanceof String)) return false;
                String str = (String)value;
                if (key.equals("type")) {
                    if (!str.equals("reply") && !str.equals("push")
                        && !str.equals("transition"))
                        return false;
                }
                break;

            default:
                return false;
        }
        return true;
    }

    /**
     * get a field of FormatterMessageJSON
     * @param key String of key
     * @return The corresponding value of key in Object
     *         Return null if no such key
     */
    public Object get(String key) {
        if (!isKey(key)) {
            log.info(String.format("%s is not a valid key", key));
            return null;
        } else {
            Object ret = null;
            try {
                ret = jo.get(key);
            } catch (JSONException e) {
                log.info("Valid but nonexisting key {}", key);
            }
            return ret;
        }
    }

    /**
     * set a field of FormatterMessageJSON
     * @param key String of key, only some values are allowed
     * @param value Object to be set
     * @return This object
     */
    public FormatterMessageJSON set(String key, Object value) {
        if (!isKey(key)) {
            log.info(String.format("%s is not a valid key", key));
        } else {
            if (validateKeyValue(key, value))
                jo.put(key, value);
        }
        return this;
    }

    /**
     * Append a text message to `messages` field
     * @param text String of text content
     * @return This object
     */
    public FormatterMessageJSON appendTextMessage(String text) {
        JSONObject msg = new JSONObject();
        msg.put("type", "text");
        msg.put("textContent", text);
        jo.append("messages", msg);
        return this;
    }

    /**
     * Append an image message to `messages` field
     * @param originalContentUrl URL to original image
     * @param previewContentUrl URL to preview image (may be the same as
     *                          originalContentUrl)
     * @return This object
     */
    public FormatterMessageJSON appendImageMessage(String originalContentUrl,
                                   String previewContentUrl) {
        JSONObject msg = new JSONObject();
        msg.put("type", "image");
        msg.put("originalContentUrl", originalContentUrl);
        msg.put("previewContentUrl", previewContentUrl);
        jo.append("messages", msg);
        return this;
    }

    /**
     * Return a pretty formatted JSON
     */
    @Override
    public String toString() {
        return "\n"+jo.toString(4);
    }
}