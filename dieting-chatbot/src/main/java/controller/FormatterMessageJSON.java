package controller;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;

@Component
public class FormatterMessageJSON {
    static private HashSet<String> keySet;
    {
        keySet = new HashSet<String>();
        keySet.add("type");
        keySet.add("userId");
        keySet.add("replyToken");
        keySet.add("messages");
        keySet.add("stateTransition");
    }

    // private String type = "push";    // "reply" or "push"
    // private String userId = "NULL";  
    // private String replyToken = "314159";
    // private JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
    // JsonArray messages;
    // String stateTransition;

    private JSONObject jo;
    {
        jo.put("type", "push");
        jo.put("userId", "NULL");
        jo.put("messages", new JSONArray());
    }

    private boolean isKey(String key) {
        return keySet.contains(key);
    }

    public Object get(String key) {
        if (!isKey(key)) {
            log.info(String.format("%s is not a valid key", key));
            return null;
        } else {
            return jo.get(key);
        }
    }

    public void set(String key, Object value) {
        if (!isKey(key)) {
            log.info(String.format("%s is not a valid key", key));
            return null;
        } else {
            switch (key) {
                case "type":
                if (!value.equals("reply") && !value.equals)
            }
        }
    }
    /*
    public String getType() {
        return type;
    }
    public String getUserId() {
        return userId;
    }
    public String getReplyToken() {
        return replyToken;
    }
    public String getStateTransition() {
        return stateTransition;
    }
    public JsonArray getMessages() {
        return messages;
    }
    */

    /*
    public void setType(String type) {
        if(type.equals("reply") || type.equals("push")) {
            this.type = type;
        } else {
            log.info(String.format("Invalid value %s for type", type));
        }
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public void setReplyToken(String replyToken) {
        this.replyToken = replyToken;
    }
    public void setStateTransition(String stateTransition) {
        this.stateTransition = stateTransition;
    }

    public void addTextMessage(@Nullable String id, String textContent) {
        if(messages == null || messages.size() < 5) 
            jsonArrayBuilder.add(Json.createObjectBuilder().add("type", "text")
                .add("id", id==null?"NULL":id).add("textContent", textContent)
                .build());
    }

    public void addFormatterImageMessage(String originalContentUrl, String previewContentUrl) {
        if(messages == null || messages.size() < 5)
            jsonArrayBuilder.add(Json.createObjectBuilder().add("type", "image")
                .add("originalContentUrl", originalContentUrl)
                .add("previewContentUrl", previewContentUrl).build());
    }
    public void buildArray() {
        messages = jsonArrayBuilder.build();
    }
    */
}