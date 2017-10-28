package controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.json.Json;
import javax.json.JsonObject;
import javax.annotation.Nullable;
import org.json.JSONObject;
import org.mockito.internal.stubbing.answers.ThrowsException;
import org.springframework.stereotype.Component;

@Component
public class ParserMessageJSON {
    static private HashSet<String> keySet;
    {
        keySet = new HashSet<String>();
        keySet.add("userId");
        keySet.add("state");
        keySet.add("replyToken");
        keySet.add("message");
    }
    /*
    String userId;
    String state;
    String replyToken;
    JsonObject message; 
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public void setState(String state) {
        this.state = state;
    }
    public void setReplyToken(String replyToken) {
        this.replyToken = replyToken;
    }

    public void addTextMessage(@Nullable String id, String textContent) {
        message = Json.createObjectBuilder().add("type", "text")
          .add("id", id==null?"NULL":id).add("textContent", textContent).build();
    }
    
    public void addParserImageMessage(String id) {
        message = Json.createObjectBuilder().add("type", "image").add("id", id).build();
    }
    

    public String getUserId() {
        return userId;
    }
    public String getState() {
        return state;
    }
    public String getReplyToken() {
        return replyToken;
    }
    public JsonObject getMessage() {
        return message;
    }

    */

    private JSONObject jo;
    {
        jo.put("type", "push");
        jo.put("id", "NULL");
    }

    private boolean isKey(String key) {
        return keySet.contains(key);
    }

    public Object get(String key) throws Exception {
        if (!isKey(key)){
            throw new Exception("Invalid key");
        } else {
            return jo.get(key); 
        }
    }

    public ParserMessageJSON set(String key, Object value) throws Exception {
        if (!isKey(key)) {
            throw new Exception("Invalid key"); 
        } else {
            jo.put(key, value); 
        }
        return this;
    }

}
