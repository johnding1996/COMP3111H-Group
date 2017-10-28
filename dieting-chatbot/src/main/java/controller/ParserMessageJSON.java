package controller;

import java.util.HashSet;
import org.json.JSONObject;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import controller.StateMachine;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ParserMessageJSON {
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
                    if (!StateMachine.isValidState(str)) return false;
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
    public Object get(String key) {
        if (!isKey(key)) {
            log.info(String.format("%s is not a valid key", key));
            return null;
        } else {
            return jo.get(key);
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
     * Return a pretty formatted JSON
     */
    @Override
    public String toString() {
        return "\n"+jo.toString(4);
    }
}