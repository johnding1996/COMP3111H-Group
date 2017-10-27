package com.example.bot.spring;
import org.json.JSONArray;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;

@Component
public class FormatterMessageJSON {
    String type;    // reply or push
    String userId;  
    String replyToken;
    //List<MsgJSON> messages = new ArrayList<MsgJSON>();
    JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
    JsonArray messages;
    String stateTransition;

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

    public void setType(String type) {
        try{
            if(type == "text" || type == "image") {
                this.type = type;
            } else {
				throw new Exception("Invalid Type !!!");
            } 
            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
		}
    }

    public void addTextMessage(String id, String textContent) {
        if(messages == null || messages.size() < 5) 
            jsonArrayBuilder.add(Json.createObjectBuilder().add("type", "text").add("id", id).add("textContent", textContent).build());
    }
    public void addFormatterImageMessage(String originalContentUrl, String previewContentUrl) {
        if(messages == null || messages.size() < 5)
            jsonArrayBuilder.add(Json.createObjectBuilder().add("type", "image").add("originalContentUrl", originalContentUrl).add("previewContentUrl", previewContentUrl).build());
    }
    public void buildArray() {
        messages = jsonArrayBuilder.build();
    }
    /*
    public void setMessage(MsgJSON msg) {
        if(messages.size() < 5)
            messages.add(msg);
    }
    */
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public void setReplyToken(String replyToken) {
        this.replyToken = replyToken;
    }
    public void setStateTransition(String stateTransition) {
        this.stateTransition = stateTransition;
    }
}