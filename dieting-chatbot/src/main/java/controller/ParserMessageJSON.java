package controller;

import java.util.ArrayList;
import java.util.List;
import javax.json.Json;
import javax.json.JsonObject;
import javax.annotation.Nullable;

import org.springframework.stereotype.Component;

@Component
public class ParserMessageJSON {
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

}
