package com.example.bot.spring;
import org.json.JSONArray;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@Component
public class FormatterMessageJSON {
    String type;    // reply or push
    String userId;  
    String replyToken;
    List<MsgJSON> messages = new ArrayList<MsgJSON>();
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
    public List<MsgJSON> getMessages() {
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

    public void setMessage(MsgJSON msg) {
        if(messages.size() < 5)
            messages.add(msg);
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
}