package com.example.bot.spring;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ParserMessageJSON {
    String userId;
    String state;
    String replyToken;
    List<MsgJSON> message = new ArrayList<MsgJSON>(); 
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public void setState(String state) {
        this.state = state;
    }
    public void setReplyToken(String replyToken) {
        this.replyToken = replyToken;
    }
    public void setMessage(MsgJSON msg) {
        message.add(msg);
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
    public List<MsgJSON> getMessages() {
        return message;
    }

}